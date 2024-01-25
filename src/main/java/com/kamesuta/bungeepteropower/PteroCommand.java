package com.kamesuta.bungeepteropower;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;
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
            sender.sendMessage(plugin.messages.warning("ptero_usage"));
            return;
        }

        String subCommand = args[0];
        switch (subCommand) {
            case "reload":
                // Permission check
                if (!sender.hasPermission("ptero.reload")) {
                    sender.sendMessage(plugin.messages.error("insufficient_permission"));
                    return;
                }

                // Reload config.yml
                plugin.reload();
                sender.sendMessage(plugin.messages.success("config_reloaded"));

                break;

            case "start":
            case "stop": {
                // args[1] is the server name
                if (args.length < 2) {
                    sender.sendMessage(plugin.messages.warning("ptero_" + subCommand + "_usage"));
                    return;
                }
                String serverName = args[1];

                // Permission check
                if (!sender.hasPermission("ptero." + subCommand + "." + serverName)) {
                    sender.sendMessage(plugin.messages.error("insufficient_permission"));
                    return;
                }

                // Stop server
                String serverId = plugin.config.getServerId(serverName);
                if (serverId == null) {
                    sender.sendMessage(plugin.messages.error("server_not_configured", serverName));
                    return;
                }

                // Signal
                PterodactylAPI.PowerSignal signal = subCommand.equals("start")
                        ? PterodactylAPI.PowerSignal.START
                        : PterodactylAPI.PowerSignal.STOP;

                // Send signal
                PterodactylAPI.sendPowerSignal(serverName, serverId, signal).thenRun(() -> {
                    sender.sendMessage(plugin.messages.success("server_" + subCommand, serverName));

                    // Get the auto stop time
                    Integer autoStopTime = plugin.config.getAutoStopTime(serverName);
                    if (autoStopTime != null && autoStopTime >= 0) {
                        // Stop the server after a while
                        plugin.delay.stopAfterWhile(serverName, autoStopTime);
                        // Send message
                        sender.sendMessage(plugin.messages.warning("join_autostart_warning", serverName, autoStopTime));
                    }

                }).exceptionally(e -> {
                    sender.sendMessage(plugin.messages.error("failed_to_" + subCommand + "_server", serverName));
                    return null;
                });

                break;
            }

            default: {
                sender.sendMessage(plugin.messages.warning("ptero_usage"));
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
}