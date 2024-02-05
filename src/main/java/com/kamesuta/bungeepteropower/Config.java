package com.kamesuta.bungeepteropower;

import com.kamesuta.bungeepteropower.api.PowerController;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Plugin Configurations
 */
public class Config {
    /**
     * Required config version
     * Increments when a property that requires manual modification is added to the config
     */
    public static final int CONFIG_VERSION = 1;

    /**
     * Config version
     */
    public final int configVersion;
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
     * The type of the power controller
     * (e.g. "pterodactyl")
     */
    public final String powerControllerType;
    /**
     * The number of seconds the plugin will try to connect the player to the desired server
     * Set this to the maximum time the server can take to start
     */
    public final int startupJoinTimeout;
    /**
     * The number of seconds between pings to check the server status
     */
    public final int pingInterval;
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
            this.configVersion = configuration.getInt("version", 0);
            this.language = configuration.getString("language");
            this.startTimeout = configuration.getInt("startTimeout");
            this.powerControllerType = configuration.getString("powerControllerType");

            // Startup join settings
            this.startupJoinTimeout = configuration.getInt("startupJoin.timeout");
            this.pingInterval = configuration.getInt("startupJoin.pingInterval");

            // Pterodactyl API credentials
            this.pterodactylUrl = new URI(configuration.getString("pterodactyl.url"));
            this.pterodactylApiKey = configuration.getString("pterodactyl.apiKey");

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

    /**
     * Get the Bungeecord server names.
     *
     * @return The Bungeecord server names
     */
    public Set<String> getServerNames() {
        return serverIdMap.keySet();
    }

    private static File makeConfig() throws IOException {
        // Create the data folder if it does not exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // Create config.yml if it does not exist
        return copyFileToDataFolder("config.yml");
    }

    /**
     * Copy a file from the plugin's resources to the data folder
     *
     * @param fileName The file name in the data folder
     * @return The file
     * @throws IOException If an I/O error occurs
     */
    public static File copyFileToDataFolder(String fileName) throws IOException {
        // Create config.yml if it does not exist
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try (InputStream in = plugin.getResourceAsStream(fileName)) {
                Files.copy(in, file.toPath());
            }
        }
        return file;
    }

    /**
     * Get power controller by name
     *
     * @return The power controller, or null if not found
     */
    public PowerController getPowerController() {
        Objects.requireNonNull(powerControllerType, "Power controller type is not set");
        PowerController powerController = plugin.powerControllers.get(powerControllerType);
        Objects.requireNonNull(powerController, "No power controller found for type: " + powerControllerType);
        return powerController;
    }

    /**
     * Validate configuration
     */
    public void validateConfig(CommandSender sender) {
        // Validate the config version
        if (configVersion != CONFIG_VERSION) {
            sender.sendMessage(plugin.messages.prefix().append(String.format("Warning: Your config.yml is outdated (required version: %d, your version: %d).", CONFIG_VERSION, configVersion)).create());
            try {
                // Create/Overwrite config.new.yml
                File file = new File(plugin.getDataFolder(), "config.new.yml");
                try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                sender.sendMessage(plugin.messages.prefix().append("Warning: Check the new config.new.yml and update your config.yml. After that, set the 'version' to " + CONFIG_VERSION + ".").create());
            } catch (IOException e) {
                sender.sendMessage(plugin.messages.prefix().append("Warning: Check the new config.yml in plugin jar file and update your config.yml. After that, set the 'version' to " + CONFIG_VERSION + ".").create());
            }
        }

        // Validate the pterodactyl URL
        if (pterodactylUrl == null) {
            sender.sendMessage(plugin.messages.prefix().append("Warning: The Pterodactyl URL in the configuration is not set.").create());
        }
        if (pterodactylUrl.getHost().endsWith(".example.com")) {
            sender.sendMessage(plugin.messages.prefix().append("Warning: The Pterodactyl URL in the configuration is example.com. Please set the correct URL.").create());
        }
        // Validate the pterodactyl API key
        if (pterodactylApiKey == null) {
            sender.sendMessage(plugin.messages.prefix().append("Warning: The Pterodactyl API key in the configuration is not set.").create());
        }
        if (!pterodactylApiKey.startsWith("ptlc_")) {
            sender.sendMessage(plugin.messages.prefix().append("Warning: The Pterodactyl API key should start with 'ptlc_'.").create());
        }
        if (pterodactylApiKey.startsWith("ptlc_0000")) {
            sender.sendMessage(plugin.messages.prefix().append("Warning: The Pterodactyl API key in the configuration is the default key. Please set the correct key.").create());
        }

        // Validate the server names
        Map<String, ServerInfo> bungeecordServerNames = ProxyServer.getInstance().getServers();
        List<String> invalidServerNames = getServerNames().stream()
                .filter(serverName -> !bungeecordServerNames.containsKey(serverName))
                .collect(Collectors.toList());
        if (!invalidServerNames.isEmpty()) {
            sender.sendMessage(plugin.messages.prefix().append(String.format("Warning: The following server names in the configuration are not found in the BungeeCord server list: %s", String.join(", ", invalidServerNames))).create());
        }

        // Check if the power controller is registered
        if (plugin.config.powerControllerType == null) {
            sender.sendMessage(plugin.messages.prefix().append("Warning: The power controller type in the configuration is not set.").create());
        }
        if (plugin.powerControllers.get(plugin.config.powerControllerType) == null) {
            sender.sendMessage(plugin.messages.prefix().append(String.format("Warning: The power controller type '%s' in the configuration is not registered.", plugin.config.powerControllerType)).create());
        }
    }
}
