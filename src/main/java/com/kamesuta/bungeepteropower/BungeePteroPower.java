package com.kamesuta.bungeepteropower;

import com.kamesuta.bungeepteropower.api.BungeePteroPowerAPI;
import com.kamesuta.bungeepteropower.api.PowerController;
import com.kamesuta.bungeepteropower.power.PterodactylController;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The BungeePteroPower plugin.
 */
public final class BungeePteroPower extends Plugin implements BungeePteroPowerAPI {
    public static Logger logger;
    public static BungeePteroPower plugin;

    /**
     * Plugin Configurations
     */
    public Config config;
    /**
     * Fallback Translations
     */
    public Messages fallbackMessages;
    /**
     * Translations
     */
    public Messages messages;
    /**
     * Delayed stop task manager
     */
    public DelayManager delay;
    /**
     * Power controllers
     */
    public Map<String, PowerController> powerControllers;

    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();

        // Logo and version (To make it easier to find config warnings)
        logger.info("\n" +
                "___  _  _ _  _ ____ ____ ____ ___  ___ ____ ____ ____ ___  ____ _ _ _ ____ ____ \n" +
                "|__] |  | |\\ | | __ |___ |___ |__]  |  |___ |__/ |  | |__] |  | | | | |___ |__/ \n" +
                "|__] |__| | \\| |__] |___ |___ |     |  |___ |  \\ |__| |    |__| |_|_| |___ |  \\ \n" +
                "                    BungeePteroPower v" + getDescription().getVersion() + " by Kamesuta\n");

        // Load messages.yml
        fallbackMessages = Messages.loadFromResource("en");
        // Load config and translations
        reload();

        // Create PowerController map and register PterodactylController
        powerControllers = new ConcurrentHashMap<>();
        powerControllers.put("pterodactyl", new PterodactylController());

        // Check config
        config.validateConfig(getProxy().getConsole());

        // Create DelayManager
        delay = new DelayManager();

        // Plugin startup logic
        getProxy().getPluginManager().registerListener(this, new PlayerListener());
        // Register the /ptero reload command
        getProxy().getPluginManager().registerCommand(this, new PteroCommand());
    }

    /**
     * Load configurations and translations.
     */
    public void reload() {
        // Load config.yml
        config = new Config();

        // Load messages.yml
        messages = Messages.load(config.language, fallbackMessages);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public void registerPowerController(String name, PowerController controller) {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(controller, "Controller cannot be null");
        powerControllers.put(name, controller);
    }

    @Override
    public void unregisterPowerController(String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        powerControllers.remove(name);
    }
}
