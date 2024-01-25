package com.kamesuta.bungeepteropower;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
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

    public static final String Prefix = "[Ptero] ";
    private static final BaseComponent[] InsufficientPermissionMessage = new ComponentBuilder(Prefix + "Insufficient permission.").color(ChatColor.RED).create();

    public PteroCommand() {
        super("ptero");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new ComponentBuilder(Prefix + "Usage: /ptero <start|stop|reload>").color(ChatColor.YELLOW).create());
            return;
        }

        String subCommand = args[0];
        switch (subCommand) {
            case "reload":
                // Permission check
                if (!sender.hasPermission("ptero.reload")) {
                    sender.sendMessage(InsufficientPermissionMessage);
                    return;
                }

                // Reload config.yml
                plugin.config = PteroConfig.loadConfig();
                sender.sendMessage(new ComponentBuilder(Prefix + "Configuration reloaded.").color(ChatColor.GREEN).create());

                break;

            case "start":
            case "stop": {
                // args[1] is the server name
                if (args.length < 2) {
                    sender.sendMessage(new ComponentBuilder(Prefix + "Usage: /ptero " + subCommand + " <server>").color(ChatColor.YELLOW).create());
                    return;
                }
                String serverName = args[1];

                // Permission check
                if (!sender.hasPermission("ptero." + subCommand + "." + serverName)) {
                    sender.sendMessage(InsufficientPermissionMessage);
                    return;
                }

                // Stop server
                String serverId = plugin.config.getServerId(serverName);
                if (serverId == null) {
                    sender.sendMessage(new ComponentBuilder(Prefix + "Server " + serverName + " is not configured.").color(ChatColor.RED).create());
                    return;
                }

                // Signal
                PterodactylAPI.PowerSignal signal = subCommand.equals("start")
                        ? PterodactylAPI.PowerSignal.START
                        : PterodactylAPI.PowerSignal.STOP;
                String doing = subCommand.equals("start") ? "starting" : "stopping";

                // Send signal
                plugin.config.pterodactyl.sendPowerSignal(serverName, serverId, signal).thenRun(() -> {
                    sender.sendMessage(new ComponentBuilder(String.format(Prefix + "Server %s is %s", serverName, doing)).color(ChatColor.GREEN).create());
                }).exceptionally(e -> {
                    sender.sendMessage(new ComponentBuilder(String.format(Prefix + "Failed to %s server %s", doing, serverName)).color(ChatColor.RED).create());
                    return null;
                });
                sender.sendMessage(new ComponentBuilder(String.format(Prefix + "Send %s signal to server %s", signal.signal, serverName)).color(ChatColor.GREEN).create());

                break;
            }

            default: {
                sender.sendMessage(new ComponentBuilder(Prefix + "Usage: /ptero <start|stop|reload>").color(ChatColor.YELLOW).create());
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