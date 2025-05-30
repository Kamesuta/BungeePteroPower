package com.kamesuta.bungeepteropower;

import com.kamesuta.bungeepteropower.api.PowerController;
import com.kamesuta.bungeepteropower.api.PowerSignal;
import com.kamesuta.bungeepteropower.api.PowerStatus;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * The event listener.
 * Listens player events for auto start and stop the server.
 */
public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ProxyServer instance = ProxyServer.getInstance();

        // Call permission check for the all servers to register the permission to LuckPerms
        for (ServerInfo server : instance.getServers().values()) {
            player.hasPermission("ptero.autostart." + server.getName());
            player.hasPermission("ptero.start." + server.getName());
            player.hasPermission("ptero.stop." + server.getName());
        }

        // If the player has the permission to reload the config, notice update if available
        if (player.hasPermission("ptero.reload")) {
            // Show update message
            if (plugin.updateChecker.isUpdateAvailable()) {
                player.sendMessage(new ComponentBuilder()
                        .append(plugin.messages.info("update_available", plugin.updateChecker.getRunningVersion(), plugin.updateChecker.getNewVersion()))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.messages.getMessage("update_available_tooltip", plugin.updateChecker.getRunningVersion(), plugin.updateChecker.getNewVersion()))))
                        .event(new ClickEvent(ClickEvent.Action.OPEN_URL, plugin.updateChecker.getDownloadLink()))
                        .create()
                );
            }
        }
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        // Get the target server
        ServerInfo targetServer = event.getTarget();
        ProxiedPlayer player = event.getPlayer();

        // Cancel the task to stop the server
        String serverName = targetServer.getName();
        plugin.delay.cancelStop(serverName);

        // Permission check
        boolean autostart = player.hasPermission("ptero.autostart." + serverName);
        boolean start = player.hasPermission("ptero.start." + serverName);
        if (!autostart && !start) {
            return;
        }

        // If anyone is connected to the target server, nothing needs to be done
        if (!targetServer.getPlayers().isEmpty()) {
            return;
        }

        // Get the Pterodactyl server ID
        Config.ServerConfig server = plugin.config.getServerConfig(serverName);
        if (server == null) {
            return;
        }

        // Check if the event is a join event
        boolean isLogin = event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY;
        // Send pings to the server synchronously
        boolean useSynchronousPing = isLogin && plugin.config.useSynchronousPing;

        // Ping the target server and check if it is offline
        CompletableFuture<Void> offlineFuture = checkOnline(targetServer, server)
                .thenAccept(isOnline -> {
                    // If the server is offline, turn on the server
                    if (!isOnline) {
                        handleOffline(player, serverName, server, useSynchronousPing, event);
                    }
                })
                .exceptionally((Throwable e) -> {
                    logger.log(Level.WARNING, "Failed to ping server: " + serverName, e);
                    return null;
                });

        if (useSynchronousPing) {
            // In synchronous mode, block
            try {
                offlineFuture.get();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to wait for ping: " + serverName, e);
            }
        }

    }

    /**
     * Helper method to check if the server is online.
     * This method switches between two different ways to check the server status:
     * 1. BungeeCord ping (callback): Uses BungeeCord's built-in ping mechanism
     * 2. Panel API (Future): Uses the panel's API to check the server status
     *
     * @param targetServer The target server
     * @param server       The server configuration
     * @return A future that completes with true if the server is online, false otherwise
     */
    private CompletableFuture<Boolean> checkOnline(
            ServerInfo targetServer,
            Config.ServerConfig server
    ) {
        try {
            if ("bungeecord".equals(plugin.config.serverStatusCheckMethod)) {
                CompletableFuture<Boolean> future = new CompletableFuture<>();
                targetServer.ping((result, error) -> {
                    // Consider online if error is null
                    future.complete(error == null);
                });
                return future;
            } else { // "panel" method
                // Call checkPowerStatus implemented in PowerController
                PowerController powerController = plugin.config.getPowerController();
                return powerController.checkPowerStatus(targetServer.getName(), server.id)
                        .thenApply(status -> status == PowerStatus.RUNNING);
            }
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Handles when a player attempts to connect to a server that is offline.
     *
     * @param player             The player attempting to connect.
     * @param serverName         The name of the server the player is trying to connect to.
     * @param server             The configuration of the target server.
     * @param useSynchronousPing Whether synchronous ping is enabled to handle custom messages.
     * @param event              The server connection event triggered by the player.
     */
    private void handleOffline(
            ProxiedPlayer player,
            String serverName,
            Config.ServerConfig server,
            boolean useSynchronousPing,
            ServerConnectEvent event
    ) {
        // Permission check
        if (player.hasPermission("ptero.autostart." + serverName)) {
            // If synchronous ping is enabled, we can disconnect the player to show a custom message instead of "Could not connect to a default or fallback server".
            if (useSynchronousPing) {
                // Disconnect the player to show custom message
                player.disconnect(new ComponentBuilder(plugin.messages.getMessage("join_autostart_login", serverName)).color(ChatColor.YELLOW).create());
            } else {
                // Send title and message
                player.sendTitle(ProxyServer.getInstance().createTitle()
                        .title(new ComponentBuilder(plugin.messages.getMessage("join_autostart_title", serverName)).color(ChatColor.YELLOW).create())
                        .subTitle(new ComponentBuilder(plugin.messages.getMessage("join_autostart_subtitle", serverName)).create())
                );
            }

            // Send power signal
            ServerController.sendPowerSignal(player, serverName, server, PowerSignal.START);

            // Record statistics
            plugin.statistics.actionCounter.increment(Statistics.ActionCounter.ActionType.START_SERVER_AUTOJOIN);
            plugin.statistics.startReasonRecorder.recordStart(serverName, Statistics.StartReasonRecorder.StartReason.AUTOJOIN);

            // If synchronous ping is enabled, we can suppress "Could not connect to a default or fallback server" message
            if (useSynchronousPing) {
                event.setCancelled(true);
            }

        } else {
            // Send message including the command to start the server
            player.sendMessage(plugin.messages.warning("join_start", serverName));
            player.sendMessage(new ComponentBuilder()
                    .append(plugin.messages.success("join_start_button", serverName))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ptero start " + serverName))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.messages.getMessage("join_start_button_tooltip", serverName))))
                    .color(ChatColor.GREEN)
                    .create());

        }
    }

    @EventHandler(priority = (byte) 1024)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // Called when a player disconnect from proxy IN the target server
        Server server = event.getPlayer().getServer();
        if (server == null) {
            // Called when a player is kicked by a plugin or similar before joining the server (ex. VPNCheck plugin)
            return;
        }
        ServerInfo targetServer = server.getInfo();

        onPlayerQuit(event.getPlayer(), targetServer);
    }

    @EventHandler(priority = (byte) 1024)
    public void onPlayerKicked(ServerKickEvent event) {
        // Called when a player disconnect from proxy IN the target server
        ServerInfo targetServer = event.getKickedFrom();

        onPlayerQuit(event.getPlayer(), targetServer);
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        // Called when a player switch the server
        ServerInfo targetServer = event.getFrom();
        if (targetServer == null) {
            // Called when a player join the proxy
            return;
        }

        onPlayerQuit(event.getPlayer(), targetServer);
    }

    /**
     * Called when a player quits the target server.
     *
     * @param player       The player who quit the target server
     * @param targetServer The target server
     */
    private void onPlayerQuit(ProxiedPlayer player, ServerInfo targetServer) {
        // If you are last player on the target server, stop the server after a while
        // Check if the server is empty, or only you are on the server
        if (!(targetServer.getPlayers().isEmpty()
                || targetServer.getPlayers().size() == 1 && targetServer.getPlayers().contains(player))) {
            return;
        }

        // Get the auto stop time
        String serverName = targetServer.getName();
        // Get the Pterodactyl server ID
        Config.ServerConfig server = plugin.config.getServerConfig(serverName);
        if (server == null) {
            return;
        }

        // Stop the server when everyone leaves
        ServerController.stopAfterWhile(player, serverName, server, PowerSignal.STOP);
    }

}
