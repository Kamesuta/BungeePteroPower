package com.kamesuta.bungeepteropower;

import com.google.common.collect.ImmutableList;
import com.kamesuta.bungeepteropower.api.PowerSignal;
import net.md_5.bungee.api.CommandSender;
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
                Config.ServerConfig server = plugin.config.getServerConfig(serverName);
                if (server == null) {
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

                // Send signal and auto join
                ServerController.sendPowerSignal(sender, serverName, server, signal);

                // Record statistics
                if (signal == PowerSignal.START) {
                    plugin.statistics.actionCounter.increment(Statistics.ActionCounter.ActionType.START_SERVER_COMMAND);
                    plugin.statistics.startReasonRecorder.recordStart(serverName, Statistics.StartReasonRecorder.StartReason.COMMAND);
                } else {
                    plugin.statistics.actionCounter.increment(Statistics.ActionCounter.ActionType.STOP_SERVER_COMMAND);
                    plugin.statistics.startReasonRecorder.recordStop(serverName);
                }

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
            // /ptero <tab>
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
            // /ptero command <tab>
            String subCommand = args[0];
            if (subCommand.equalsIgnoreCase("start") || subCommand.equalsIgnoreCase("stop")) {
                // Complete server names that the sender has permission to
                return plugin.config.getServerNames().stream()
                        .filter(name -> name.startsWith(args[1]))
                        .filter(name -> sender.hasPermission("ptero." + subCommand + "." + name))
                        .filter(name -> plugin.config.getServerConfig(name) != null)
                        .collect(Collectors.toList());
            }

        }

        return ImmutableList.of();
    }
}