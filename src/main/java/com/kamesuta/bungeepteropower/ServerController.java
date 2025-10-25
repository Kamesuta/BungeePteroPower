package com.kamesuta.bungeepteropower;

import com.kamesuta.bungeepteropower.api.PowerController;
import com.kamesuta.bungeepteropower.api.PowerSignal;
import com.kamesuta.bungeepteropower.api.PowerStatus;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Provides a function to send power signals to the server and join the server when it is started
 */
public class ServerController {

    /**
     * Send a power signal to the server and join the server when it is started
     *
     * @param sender     The command sender
     * @param serverName The name of the server to send the signal
     * @param signalType The power signal to send
     */
    public static void sendPowerSignal(CommandSender sender, String serverName, PowerSignal signalType) {
        // Get the server configuration
        Config.ServerConfig server = plugin.config.getServerConfig(serverName);
        if (server == null) {
            return;
        }

        // Get signal
        String signal = signalType.getSignal();

        // Send power signal
        CompletableFuture<Void> future;
        PowerController powerController = plugin.config.getPowerController();
        if (signalType == PowerSignal.STOP && server.backupId != null && !server.backupId.isEmpty()) {
            // Restore from backup if the backup ID is specified
            future = powerController.sendRestoreSignal(serverName, server.id, server.backupId);
        } else {
            // Otherwise, send power signal
            future = powerController.sendPowerSignal(serverName, server.id, signalType);
        }

        // After the power signal is sent
        future.thenRun(() -> {
            if (signalType == PowerSignal.STOP) {
                // When stopping the server
                sender.sendMessage(plugin.messages.success("server_stop", serverName));
                return;
            }

            // Start auto stop task and send warning
            if (sender instanceof ProxiedPlayer && plugin.config.startupJoinTimeout > 0) {
                // If auto join is configured, join the server when it is started
                sender.sendMessage(plugin.messages.success("server_startup_join", serverName));

                // Wait until the server is started
                waitUntil(serverName, PowerStatus.RUNNING).thenRun(() -> {
                    // Move player to the started server
                    ProxiedPlayer player = (ProxiedPlayer) sender;
                    // Get the server info
                    ServerInfo serverInfo = plugin.getProxy().getServerInfo(serverName);
                    if (serverInfo != null) {
                        if (plugin.config.joinDelay > 0) {
                            // Delay the join
                            plugin.getProxy().getScheduler().schedule(plugin, () -> player.connect(serverInfo), plugin.config.joinDelay, TimeUnit.SECONDS);
                            // Send a message
                            player.sendMessage(plugin.messages.success("server_startup_join_move_delayed", serverName, plugin.config.joinDelay));
                        } else {
                            // Join immediately
                            player.connect(serverInfo);
                            // Send a message
                            player.sendMessage(plugin.messages.success("server_startup_join_move", serverName));
                        }
                    }
                }).exceptionally((Throwable e) -> {
                    sender.sendMessage(plugin.messages.warning("server_startup_join_failed", serverName));
                    return null;
                });

            } else {
                // Otherwise, just send a message
                sender.sendMessage(plugin.messages.success("server_start", serverName));
            }

            // Stop the server if nobody joins after a while
            stopAfterWhile(sender, serverName, signalType);

        }).exceptionally(e -> {
            sender.sendMessage(plugin.messages.error("server_" + signal + "_failed", serverName));
            return null;

        });
    }

    /**
     * Stop the server after a while
     *
     * @param sender     The command sender
     * @param serverName The name of the server to stop
     * @param signalType Is this executed while stopping or starting?
     */
    public static void stopAfterWhile(CommandSender sender, String serverName, PowerSignal signalType) {
        // Get the server configuration
        Config.ServerConfig server = plugin.config.getServerConfig(serverName);
        if (server == null) {
            return;
        }

        // Get signal
        final String signal = signalType.getSignal();

        // Get the auto stop time
        int timeout = server.timeout;
        if (timeout == 0) return;

        // When on starting, use the start timeout additionally
        if (signalType == PowerSignal.START) {
            timeout += plugin.config.startTimeout;
        }
        final int serverTimeout = timeout;

        // Stop the server after a while
        plugin.delay.stopAfterWhile(serverName, serverTimeout, () -> {
            // Stop the server
            sendPowerSignal(sender, serverName, PowerSignal.STOP);

            // Record statistics
            plugin.statistics.actionCounter.increment(Statistics.ActionCounter.ActionType.STOP_SERVER_NOBODY);
            plugin.statistics.startReasonRecorder.recordStop(serverName);
        });

        // Check if the server is actually running before sending the warning message
        checkPowerStatus(serverName).thenAccept(status -> {
            // Only send message if the server is running
            if (status != PowerStatus.OFFLINE) {
                sender.sendMessage(plugin.messages.warning("server_" + signal + "_warning", serverName, serverTimeout));
            }
        }).exceptionally(e -> {
            logger.warning("Failed to check server status for stopAfterWhile message: " + serverName);
            return null;
        });
    }

    /**
     * Wait until the server reaches the specified power status
     *
     * @param serverName   The name of the server
     * @param targetStatus The power status to wait for
     * @return A future that completes when the server reaches the target status
     */
    public static CompletableFuture<Void> waitUntil(String serverName, PowerStatus targetStatus) {
        CompletableFuture<Void> future = new CompletableFuture<Void>().orTimeout(plugin.config.startupJoinTimeout, TimeUnit.SECONDS);
        // Wait until the server reaches the target status
        Consumer<PowerStatus> callback = new Consumer<>() {
            @Override
            public void accept(PowerStatus status) {
                // Do nothing if timeout or already completed
                if (future.isDone()) {
                    return;
                }
                // Complete if the server has reached the target status
                if (status == targetStatus) {
                    future.complete(null);
                    return;
                }
                // Otherwise schedule another ping
                logger.fine("Server is not in target state. Current state: " + status + ", Target state: " + targetStatus + " for server: " + serverName);
                plugin.getProxy().getScheduler().schedule(plugin, () -> checkPowerStatus(serverName).thenAccept(this), plugin.config.pingInterval, TimeUnit.SECONDS);
            }
        };
        // Initial check
        checkPowerStatus(serverName).thenAccept(callback);

        return future;
    }

    /**
     * Check the server status.
     * This method switches between two different ways to check the server status:
     * 1. BungeeCord ping (callback): Uses BungeeCord's built-in ping mechanism
     * 2. Panel API (Future): Uses the panel's API to check the server status
     *
     * @param serverName The name of the server
     * @return A future that completes with the power status of the server
     */
    public static CompletableFuture<PowerStatus> checkPowerStatus(String serverName) {
        try {
            // Get the server configuration
            Config.ServerConfig server = plugin.config.getServerConfig(serverName);
            if (server == null) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Server not found: " + serverName));
            }

            if ("bungeecord".equals(plugin.config.serverStatusCheckMethod)) {
                // Get the server info
                ServerInfo targetServer = plugin.getProxy().getServerInfo(serverName);
                if (targetServer == null) {
                    return CompletableFuture.failedFuture(new IllegalArgumentException("ServerInfo not found: " + serverName));
                }

                CompletableFuture<PowerStatus> future = new CompletableFuture<>();
                targetServer.ping((result, error) -> {
                    // Consider online if error is null
                    future.complete(error == null ? PowerStatus.RUNNING : PowerStatus.OFFLINE);
                });
                return future;
            } else { // "panel" method
                // Call checkPowerStatus implemented in PowerController
                PowerController powerController = plugin.config.getPowerController();
                return powerController.checkPowerStatus(serverName, server.id);
            }
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

}
