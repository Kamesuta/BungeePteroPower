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
    /**
     * The Pterodactyl API client.
     */
    public final PterodactylAPI pterodactyl;
    /**
     * When no one enters the server after starting the server,
     * the server will be stopped after this time has elapsed according to the autostop setting.
     */
    public final int noPlayerTimeoutTime;

    /**
     * Pterodactyl server ID
     */
    private final Map<String, String> serverIdMap;
    /**
     * The time in seconds to stop the server after the last player leaves.
     */
    private final Map<String, Integer> serverAutoStopMap;

    private PteroConfig(PterodactylAPI pterodactyl, int noPlayerTimeoutTime, Map<String, String> serverIdMap, Map<String, Integer> serverAutoStopMap) {
        this.pterodactyl = pterodactyl;
        this.noPlayerTimeoutTime = noPlayerTimeoutTime;
        this.serverIdMap = serverIdMap;
        this.serverAutoStopMap = serverAutoStopMap;
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
    public @Nullable Integer getAutoStopTime(String serverName) {
        return serverAutoStopMap.get(serverName);
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
            // Create Pterodactyl API client
            URI pterodactylUrl = new URI(configuration.getString("pterodactyl.url"));
            String pterodactylToken = configuration.getString("pterodactyl.token");
            PterodactylAPI pterodactyl = new PterodactylAPI(pterodactylUrl, pterodactylToken);

            int noPlayerTimeoutTime = configuration.getInt("noPlayerTimeoutTime");

            // Bungeecord server name -> Pterodactyl server ID list
            HashMap<String, String> idMap = new HashMap<>();
            HashMap<String, Integer> autoStopMap = new HashMap<>();
            Configuration servers = configuration.getSection("servers");
            for (String serverId : servers.getKeys()) {
                Configuration section = servers.getSection(serverId);
                idMap.put(serverId, section.getString("id"));
                autoStopMap.put(serverId, section.getInt("autostop"));
            }

            // Create PteroConfig
            return new PteroConfig(pterodactyl, noPlayerTimeoutTime, idMap, autoStopMap);

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
