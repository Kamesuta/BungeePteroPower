package com.kamesuta.bungeepteropower;

import com.google.common.collect.ImmutableList;
import com.kamesuta.bungeepteropower.api.PowerSignal;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * The /ptero command.
 */
public class PteroCommand extends Command implements TabExecutor {


    public PteroCommand() {
        super("ptero");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.messages.warning("command_usage"));
            return;
        }

        String subCommand = args[0];
        switch (subCommand) {
            case "reload":
                // Permission check
                if (!sender.hasPermission("ptero.reload")) {
                    sender.sendMessage(plugin.messages.error("command_insufficient_permission"));
                    return;
                }

                // Reload config.yml
                plugin.reload();
                plugin.config.validateConfig(sender);
                sender.sendMessage(plugin.messages.success("command_config_reloaded"));

                break;
            case "check":
                // Permission check
                if (!sender.hasPermission("ptero.reload")) {
                    sender.sendMessage(plugin.messages.error("command_insufficient_permission"));
                    return;
                }

                // Validate config.yml
                Config config = new Config();
                config.validateConfig(sender);
                sender.sendMessage(plugin.messages.success("command_config_checked"));

                break;
            case "start":
            case "stop": {
                // args[1] is the server name
                if (args.length < 2) {
                    sender.sendMessage(plugin.messages.warning("command_" + subCommand + "_usage"));
                    return;
                }
                String serverName = args[1];

                // Permission check
                if (!sender.hasPermission("ptero." + subCommand + "." + serverName)) {
                    sender.sendMessage(plugin.messages.error("command_insufficient_permission"));
                    return;
                }

                // Stop server
                String serverId = plugin.config.getServerId(serverName);
                if (serverId == null) {
                    sender.sendMessage(plugin.messages.error("command_server_not_configured", serverName));
                    return;
                }

                // Signal
                PowerSignal signal = subCommand.equals("start")
                        ? PowerSignal.START
                        : PowerSignal.STOP;

                // Cancel existing stop task
                if (signal == PowerSignal.STOP) {
                    plugin.delay.cancelStop(serverName);
                }

                // Send signal
                plugin.config.getPowerController().sendPowerSignal(serverName, serverId, signal).thenRun(() -> {
                    if (sender instanceof ProxyServer && plugin.config.startupJoinTimeout > 0) {
                        // If auto join is configured, join the server when it is started
                        sender.sendMessage(plugin.messages.success("command_server_start_autojoin", serverName));
                        ServerInfo serverInfo = plugin.getProxy().getServerInfo(serverName);
                        onceStarted(serverInfo).thenRun(() -> {
                            // Move player to the started server
                            ProxiedPlayer player = (ProxiedPlayer) sender;
                            player.connect(serverInfo);
                        }).exceptionally((Throwable e) -> {
                            sender.sendMessage(plugin.messages.warning("command_server_start_autojoin_warning", serverName));
                            return null;
                        });
                    } else {
                        // Otherwise, just send a message
                        sender.sendMessage(plugin.messages.success("command_server_" + subCommand, serverName));
                    }

                    // Start auto stop task and send warning
                    if (signal == PowerSignal.START) {
                        // Get the auto stop time
                        Integer serverTimeout = plugin.config.getServerTimeout(serverName);
                        if (serverTimeout != null && serverTimeout >= 0) {
                            // Stop the server after a while
                            plugin.delay.stopAfterWhile(serverName, serverTimeout);
                            // Send message
                            sender.sendMessage(plugin.messages.warning("command_server_start_warning", serverName, serverTimeout));
                        }
                    }

                }).exceptionally(e -> {
                    sender.sendMessage(plugin.messages.error("command_failed_" + subCommand, serverName));
                    return null;
                });

                break;
            }

            default: {
                sender.sendMessage(plugin.messages.warning("command_usage"));
                break;
            }
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String subCommand = args[0];
            List<String> completions = new ArrayList<>();
            String partialCommand = subCommand.toLowerCase();

            if (sender.hasPermission("ptero.reload")) {
                if ("reload".startsWith(partialCommand)) {
                    completions.add("reload");
                }
                if ("check".startsWith(partialCommand)) {
                    completions.add("check");
                }
            }

            if ("start".startsWith(partialCommand)) {
                completions.add("start");
            }

            if ("stop".startsWith(partialCommand)) {
                completions.add("stop");
            }

            return completions;

        } else if (args.length == 2) {
            String subCommand = args[0];
            if (subCommand.equalsIgnoreCase("start") || subCommand.equalsIgnoreCase("stop")) {
                return ProxyServer.getInstance().getServers().values().stream()
                        .map(ServerInfo::getName)
                        .filter(name -> name.startsWith(args[1]))
                        .filter(name -> sender.hasPermission("ptero." + subCommand + "." + name))
                        .filter(name -> plugin.config.getServerId(name) != null)
                        .collect(Collectors.toList());
            }

        }

        return ImmutableList.of();
    }

    private static CompletableFuture<Void> onceStarted(ServerInfo serverInfo) {
        CompletableFuture<Void> future = new CompletableFuture<Void>();

        // The timestamp when the server is expected to be started within
        Instant timeout = Instant.now().plusSeconds(plugin.config.startupJoinTimeout);

        Callback<ServerPing> callback = new Callback<>() {
            @Override
            public void done(ServerPing serverPing, Throwable throwable) {
                // If the server is started, complete the future
                if (throwable == null && serverPing != null) {
                    future.complete(null);
                    return;
                }
                // Not started yet, retry after a while
                if (Instant.now().isBefore(timeout)) {
                    ProxyServer.getInstance().getScheduler()
                            .schedule(plugin, () -> serverInfo.ping(this), plugin.config.pingInterval, TimeUnit.SECONDS);
                    return;
                }

                // If the server is not started within the timeout, complete the future exceptionally
                future.completeExceptionally(new RuntimeException("Server did not start in autoJoinTimeout"));
            }
        };
        serverInfo.ping(callback);
        return future;
    }
}