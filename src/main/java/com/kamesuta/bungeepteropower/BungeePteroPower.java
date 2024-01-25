package com.kamesuta.bungeepteropower;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.kamesuta.bungeepteropower.PteroCommand.Prefix;

/**
 * The BungeePteroPower plugin.
 */
public final class BungeePteroPower extends Plugin implements Listener {
    public static Logger logger;
    public static BungeePteroPower plugin;
    public PteroConfig config;
    private Map<String, ScheduledTask> serverStopTasks = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();

        // Load config.yml
        config = PteroConfig.loadConfig();

        // Plugin startup logic
        getProxy().getPluginManager().registerListener(this, this);
        // Register the /ptero reload command
        getProxy().getPluginManager().registerCommand(this, new PteroCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getProxy().getPluginManager().unregisterListener(this);
    }

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
        player.hasPermission("ptero.reload");
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        // Get the target server
        ServerInfo targetServer = event.getTarget();
        ProxiedPlayer player = event.getPlayer();
        ProxyServer instance = ProxyServer.getInstance();

        // Cancel the task to stop the server
        cancelStop(targetServer.getName());

        // Permission check
        boolean autostart = player.hasPermission("ptero.autostart." + targetServer.getName());
        boolean start = player.hasPermission("ptero.start." + targetServer.getName());
        if (!autostart && !start) {
            return;
        }

        // If anyone is connected to the target server, nothing needs to be done
        if (!targetServer.getPlayers().isEmpty()) {
            return;
        }

        // Get the Pterodactyl server ID
        String pterodactylServerId = config.getServerId(targetServer.getName());
        if (pterodactylServerId == null) {
            return;
        }

        // Ping the target server and check if it is offline
        targetServer.ping((result, error) -> {
            if (error != null) { // The server is offline
                // Start the target server
                if (autostart) {
                    // Send title and message
                    player.sendTitle(instance.createTitle()
                            .title(new ComponentBuilder("Starting Server...").create())
                            .subTitle(new ComponentBuilder("Please wait a moment.").create())
                    );
                    player.sendMessage(new ComponentBuilder(Prefix + "Server " + targetServer.getName() + " is starting... please wait a moment.").color(ChatColor.LIGHT_PURPLE).create());
                    // Send power signal
                    config.pterodactyl.sendPowerSignal(targetServer.getName(), pterodactylServerId, PterodactylAPI.PowerSignal.START);
                } else {
                    // Send message including the command to start the server
                    player.sendMessage(new ComponentBuilder(Prefix + "Server " + targetServer.getName() + " is offline but you can start it. ↓\n").color(ChatColor.RED)
                            .append(new ComponentBuilder("          [START SERVER]")
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ptero start " + targetServer.getName()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to start server")))
                                    .color(ChatColor.GREEN)
                                    .create()
                            ).create());
                }

                // Get the auto stop time
                Integer autoStopTime = config.getAutoStopTime(targetServer.getName());
                if (autoStopTime == null || autoStopTime < 0) {
                    return;
                }

                // Stop the server after a while when no one enters the server
                stopAfterWhile(targetServer.getName(), config.noPlayerTimeoutTime + autoStopTime);
            }
        });
    }

    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent event) {
        // Get the target server
        ServerInfo targetServer = event.getTarget();

        // If you are last player on the target server, stop the server after a while
        if (!targetServer.getPlayers().isEmpty()) {
            return;
        }

        // Get the auto stop time
        Integer autoStopTime = config.getAutoStopTime(targetServer.getName());
        if (autoStopTime == null || autoStopTime < 0) {
            return;
        }

        // Stop the server after a while
        stopAfterWhile(targetServer.getName(), autoStopTime);
    }

    /**
     * Stop the server after a while.
     *
     * @param serverName   The name of the server to stop
     * @param autoStopTime The time in seconds to stop the server
     */
    private void stopAfterWhile(String serverName, int autoStopTime) {
        // Log
        logger.info(String.format("Scheduled to stop server %s after %d seconds", serverName, autoStopTime));

        // Get the Pterodactyl server ID
        String pterodactylServerId = config.getServerId(serverName);
        if (pterodactylServerId == null) {
            return;
        }

        // Cancel previous task
        cancelStop(serverName);

        // Stop the server after the auto stop time
        ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(this, () -> {
            // Stop the target server
            config.pterodactyl.sendPowerSignal(serverName, pterodactylServerId, PterodactylAPI.PowerSignal.STOP);
        }, autoStopTime, TimeUnit.SECONDS);

        // Register the task
        serverStopTasks.put(serverName, task);
    }

    /**
     * Cancel the task to stop the server.
     *
     * @param serverName The name of the server to cancel stopping
     */
    private void cancelStop(String serverName) {
        // Cancel the task
        ScheduledTask task = serverStopTasks.get(serverName);
        if (task != null) {
            task.cancel();
        }
    }
}
