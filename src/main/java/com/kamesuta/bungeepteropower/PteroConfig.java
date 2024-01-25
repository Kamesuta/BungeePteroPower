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
public class PteroConfig {
    public final PterodactylAPI pterodactyl;
    private final Map<String, String> serverIdMap;

    public PteroConfig(PterodactylAPI pterodactyl, Map<String, String> serverIdMap) {
        this.pterodactyl = pterodactyl;
        this.serverIdMap = serverIdMap;
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
     * Load config.yml and initialize variables.
     *
     * @return Config instance
     */
    public static PteroConfig loadConfig() {
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
            HashMap<String, String> idMap = new HashMap<>();
            Configuration servers = configuration.getSection("servers");
            for (String serverId : servers.getKeys()) {
                String pterodactylServerId = servers.getString(serverId);
                idMap.put(serverId, pterodactylServerId);
            }

            // Create Pterodactyl API client
            PterodactylAPI pterodactyl = new PterodactylAPI(pterodactylUrl, pterodactylToken, idMap);
            // Create PteroConfig
            return new PteroConfig(pterodactyl, idMap);
        } catch (Exception e) {
            logger.severe("Failed to read config.yml");
            throw new RuntimeException(e);
        }
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
