package com.kamesuta.bungeepteropower;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.logging.Logger;

/**
 * The BungeePteroPower plugin.
 */
public final class BungeePteroPower extends Plugin implements Listener {
    public static Logger logger;
    public static BungeePteroPower plugin;
    public PteroConfig config;

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

        // Permission check
        if (!player.hasPermission("ptero.autostart." + targetServer.getName())) {
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
                player.sendTitle(instance.createTitle()
                        .title(new ComponentBuilder("Starting Server...").create())
                        .subTitle(new ComponentBuilder("Please wait a moment.").create())
                );
                config.pterodactyl.sendPowerSignal(targetServer.getName(), pterodactylServerId, PterodactylAPI.PowerSignal.START);
            }
        });
    }

    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent event) {
        logger.info("Server disconnected: " + event.getTarget().getName());
    }
}
