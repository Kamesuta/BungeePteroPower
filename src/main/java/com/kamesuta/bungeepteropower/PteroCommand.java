package com.kamesuta.bungeepteropower;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.time.Instant;

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
                sender.sendMessage(plugin.messages.success("command_config_reloaded"));

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
                PterodactylAPI.PowerSignal signal = subCommand.equals("start")
                        ? PterodactylAPI.PowerSignal.START
                        : PterodactylAPI.PowerSignal.STOP;

                // Cancel existing stop task
                if (signal == PterodactylAPI.PowerSignal.STOP)
                    plugin.delay.cancelStop(serverName);

                boolean autoJoin = subCommand.equals("start") && args.length >= 3 && args[2].equals("join") && sender instanceof ProxiedPlayer;

                // Send signal
                PterodactylAPI.sendPowerSignal(serverName, serverId, signal).thenRun(() ->{

                    if (autoJoin) {
                        sender.sendMessage(plugin.messages.success("command_server_start_autojoin", serverName));
                        ServerInfo serverInfo = plugin.getProxy().getServerInfo(serverName);
                        onceStarted(serverInfo).thenRun(()-> {
                            ProxiedPlayer player = (ProxiedPlayer)sender;
                            player.connect(serverInfo);
                        }).exceptionally((Throwable e)-> {
                            sender.sendMessage(plugin.messages.warning("command_server_start_autojoin_warning", serverName));
                            return null;
                        });
                    }
                    else
                        sender.sendMessage(plugin.messages.success("command_server_" + subCommand, serverName));

                    // Start auto stop task and send warning
                    if (subCommand.equals("start"))
                    {
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

            if ("reload".startsWith(partialCommand) && sender.hasPermission("ptero.reload")) {
                completions.add("reload");
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

    private CompletableFuture<Void> onceStarted(ServerInfo serverInfo) {
        CompletableFuture<Void> future = new CompletableFuture<Void>();

        Instant timeout = Instant.now().plusSeconds(plugin.config.autoJoinTimeout);

        Callback<ServerPing> callback = new Callback<ServerPing>() {
            @Override
            public void done(ServerPing serverPing, Throwable throwable) {
                if (throwable == null && serverPing != null) {
                    future.complete(null);
                    return;
                }
                if (Instant.now().isBefore(timeout)) {
                    serverInfo.ping(this);
                    return;
                }
                future.completeExceptionally(null);
            }
        };
        serverInfo.ping(callback);
        return future;
    }
}