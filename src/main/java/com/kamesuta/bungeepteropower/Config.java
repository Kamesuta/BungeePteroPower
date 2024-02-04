package com.kamesuta.bungeepteropower;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Plugin Configurations
 */
public class Config {
    /**
     * Language
     */
    public final String language;
    /**
     * When no one enters the server after starting the server,
     * the server will be stopped after this time has elapsed according to the timeout setting.
     */
    public final int startTimeout;
    /**
     * The number of seconds the plugin will try to connect the player to the desired server
     * Set this to the maximum time the server can take to start
     */
    public final int autoJoinTimeout;
    /**
     * Pterodactyl API URL
     */
    public final URI pterodactylUrl;
    /**
     * Pterodactyl API Key
     */
    public final String pterodactylApiKey;
    /**
     * Pterodactyl server ID
     */
    private final Map<String, String> serverIdMap;
    /**
     * The time in seconds to stop the server after the last player leaves.
     */
    private final Map<String, Integer> serverTimeoutMap;

    public Config() {
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
            // Basic settings
            this.language = configuration.getString("language");
            this.startTimeout = configuration.getInt("startTimeout");
            this.autoJoinTimeout = configuration.getInt("autoJoinTimeout");

            // Pterodactyl API credentials
            this.pterodactylUrl = new URI(configuration.getString("pterodactyl.url"));
            this.pterodactylApiKey = configuration.getString("pterodactyl.apikey");

            // Bungeecord server name -> Pterodactyl server ID list
            serverIdMap = new HashMap<>();
            serverTimeoutMap = new HashMap<>();
            Configuration servers = configuration.getSection("servers");
            for (String serverId : servers.getKeys()) {
                Configuration section = servers.getSection(serverId);
                serverIdMap.put(serverId, section.getString("id"));
                serverTimeoutMap.put(serverId, section.getInt("timeout"));
            }

        } catch (Exception e) {
            logger.severe("Failed to read config.yml");
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the Pterodactyl server ID from the Bungeecord server name.
     *
     * @param serverName The Bungeecord server name
     * @return The Pterodactyl server ID
     */
    public @Nullable String getServerId(String serverName) {
        return serverIdMap.get(serverName);
    }

    /**
     * Get auto stop time from the Bungeecord server name.
     *
     * @param serverName The Bungeecord server name
     * @return The auto stop time
     */
    public @Nullable Integer getServerTimeout(String serverName) {
        return serverTimeoutMap.get(serverName);
    }

    private static File makeConfig() throws IOException {
        // Create the data folder if it does not exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // Create config.yml if it does not exist
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            }
        }

        return file;
    }
}
