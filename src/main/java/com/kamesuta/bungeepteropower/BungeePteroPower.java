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
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * The BungeePteroPower plugin.
 */
public final class BungeePteroPower extends Plugin implements Listener {
    public static Logger logger;
    public static BungeePteroPower plugin;

    public PterodactylAPI pterodactyl;

    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();

        // Load config.yml
        loadConfig();

        // Plugin startup logic
        getProxy().getPluginManager().registerListener(this, this);
        // Register the /ptero reload command
        getProxy().getPluginManager().registerCommand(this, new PteroCommand());
    }

    /**
     * Load config.yml and initialize variables.
     */
    public void loadConfig() {
        // Create/Load config.yml
        File configFile;
        Configuration configuration;
        try {
            configFile = makeConfig();
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            logger.severe("Failed to create/load config.yml");
            throw new RuntimeException(e);
        }

        // Load config.yml
        try {
            URI pterodactylUrl = new URI(configuration.getString("pterodactyl.url"));
            String pterodactylToken = configuration.getString("pterodactyl.token");

            // Bungeecord server name -> Pterodactyl server ID list
            HashMap<String, String> serverIdMap = new HashMap<>();
            Configuration servers = configuration.getSection("servers");
            for (String serverId : servers.getKeys()) {
                String pterodactylServerId = servers.getString(serverId);
                serverIdMap.put(serverId, pterodactylServerId);
            }

            // Create Pterodactyl API client
            pterodactyl = new PterodactylAPI(pterodactylUrl, pterodactylToken, serverIdMap);
        } catch (Exception e) {
            logger.severe("Failed to read config.yml");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getProxy().getPluginManager().unregisterListener(this);
    }

    private File makeConfig() throws IOException {
        // Create the data folder if it does not exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Create config.yml if it does not exist
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            }
        }

        return file;
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ProxyServer instance = ProxyServer.getInstance();

        // Call permission check for the all servers to register the permission to LuckPerms
        for (ServerInfo server : instance.getServers().values()) {
            player.hasPermission("ptero.autostart." + server.getName());
        }
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
        String pterodactylServerId = pterodactyl.getServerId(targetServer.getName());
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
                pterodactyl.sendPowerSignal(targetServer.getName(), pterodactylServerId, PterodactylAPI.PowerSignal.START);
            }
        });
    }

    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent event) {
        logger.info("Server disconnected: " + event.getTarget().getName());
    }
}
