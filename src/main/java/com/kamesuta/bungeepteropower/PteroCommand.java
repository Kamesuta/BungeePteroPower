package com.kamesuta.bungeepteropower;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * The /ptero command.
 */
public class PteroCommand extends Command {
    public PteroCommand() {
        super("ptero");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new ComponentBuilder("Usage: /ptero <reload|start|stop>").create());
            return;
        }

        String subCommand = args[0];
        switch (subCommand) {
            case "reload":
                // Reload config.yml
                plugin.loadConfig();
                sender.sendMessage(new ComponentBuilder("Configuration reloaded.").create());

                break;

            case "start":
            case "stop": {
                // args[1] is the server name
                if (args.length < 2) {
                    sender.sendMessage(new ComponentBuilder("Usage: /ptero " + subCommand + " <server>").create());
                    return;
                }
                String serverName = args[1];

                // Stop server
                String serverId = plugin.pterodactyl.getServerId(serverName);
                if (serverId == null) {
                    sender.sendMessage(new ComponentBuilder("Server " + serverName + " is not configured.").create());
                    return;
                }

                // Signal
                PterodactylAPI.PowerSignal signal = subCommand.equals("start")
                        ? PterodactylAPI.PowerSignal.START
                        : PterodactylAPI.PowerSignal.STOP;
                String doing = subCommand.equals("start") ? "starting" : "stopping";

                // Send signal
                plugin.pterodactyl.sendPowerSignal(serverName, serverId, signal).thenRun(() -> {
                    sender.sendMessage(new ComponentBuilder(String.format("Server %s is %s", serverName, doing)).create());
                }).exceptionally(e -> {
                    sender.sendMessage(new ComponentBuilder(String.format("Failed to %s server %s", doing, serverName)).create());
                    return null;
                });
                sender.sendMessage(new ComponentBuilder(String.format("Send %s signal to server %s", signal, serverName)).create());

                break;
            }

            default: {
                sender.sendMessage(new ComponentBuilder("Usage: /ptero <reload|start|stop>").create());
                break;
            }
        }
    }
}