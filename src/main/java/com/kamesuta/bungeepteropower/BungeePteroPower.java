package com.kamesuta.bungeepteropower;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The BungeePteroPower plugin.
 */
public final class BungeePteroPower extends Plugin implements Listener {
    public static Logger logger;
    public static BungeePteroPower plugin;
    public Config config;
    public Messages messages;
    private Map<String, ScheduledTask> serverStopTasks = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();

        // Load config.yml
        config = new Config();

        // Load messages.yml
        messages = new Messages(config.language);

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
                String serverName = targetServer.getName();

                // Start the target server
                if (autostart) {
                    // Send title and message
                    player.sendTitle(instance.createTitle()
                            .title(new ComponentBuilder(messages.getMessage("join_autostart_title", serverName)).color(ChatColor.YELLOW).create())
                            .subTitle(new ComponentBuilder(messages.getMessage("join_autostart_subtitle", serverName)).create())
                    );
                    player.sendMessage(messages.info("join_autostart", serverName));
                    // Send power signal
                    config.pterodactyl.sendPowerSignal(serverName, pterodactylServerId, PterodactylAPI.PowerSignal.START);
                } else {
                    // Send message including the command to start the server
                    player.sendMessage(messages.error("join_start", serverName));
                    player.sendMessage(new ComponentBuilder()
                            .append(messages.success("join_start_button", serverName))
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ptero start " + serverName))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(messages.getMessage("join_start_button_tooltip", serverName))))
                            .color(ChatColor.GREEN)
                            .create());
                }

                // Get the auto stop time
                Integer autoStopTime = config.getAutoStopTime(serverName);
                if (autoStopTime == null || autoStopTime < 0) {
                    return;
                }

                // Stop the server after a while when no one enters the server
                stopAfterWhile(serverName, config.noPlayerTimeoutTime + autoStopTime);
                player.sendMessage(messages.info("join_autostart_warning", serverName, config.noPlayerTimeoutTime + autoStopTime));
            }
        });
    }

    @EventHandler(priority = (byte) 1024)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // Called when a player disconnect from proxy IN the target server
        ServerInfo targetServer = event.getPlayer().getServer().getInfo();

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
        Integer autoStopTime = config.getAutoStopTime(targetServer.getName());
        if (autoStopTime == null || autoStopTime < 0) {
            return;
        }

        // Stop the server after a while
        stopAfterWhile(targetServer.getName(), autoStopTime);
        // Send message
        player.sendMessage(messages.info("leave_server_stopping", targetServer.getName(), autoStopTime));
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
            // Log
            logger.info(String.format("Canceled to stop server %s", serverName));

            task.cancel();
        }
    }
}
