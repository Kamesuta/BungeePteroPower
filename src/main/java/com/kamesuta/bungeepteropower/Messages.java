package com.kamesuta.bungeepteropower;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Plugin Translations
 */
public class Messages {
    private final Configuration messages;
    private final Messages parent;

    /**
     * Create a new Messages
     *
     * @param messages Messages configuration
     * @param parent   Parent messages
     */
    private Messages(Configuration messages, Messages parent) {
        this.messages = messages;
        this.parent = parent;
    }

    /**
     * Load messages.yml
     *
     * @param language Language
     *                 If the language is not found, the parent messages will be used
     * @param parent   Parent messages
     * @return Messages
     */
    public static Messages load(String language, Messages parent) {
        try {
            // Create the data folder if it does not exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdir();
            }

            // Create messages.yml if it does not exist
            File file = Config.copyFileToDataFolder("messages_" + language + ".yml");

            // Load messages.yml
            Configuration messages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

            return new Messages(messages, parent);
        } catch (IOException e) {
            logger.severe("Failed to create/load messages.yml");
            throw new RuntimeException(e);
        }
    }

    /**
     * Load messages.yml from resource
     *
     * @param language Language
     *                 If the language is not found, the parent messages will be used
     * @param parent   Parent messages
     * @return Messages
     */
    public static Messages loadFromResource(String language, Messages parent) {
        try {
            // Load messages.yml
            try (InputStream in = plugin.getResourceAsStream("messages_" + language + ".yml")) {
                Configuration messages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(in);

                return new Messages(messages, parent);
            }
        } catch (IOException e) {
            logger.severe("Failed to load fallback messages_" + language + ".yml");
            throw new RuntimeException(e);
        }
    }

    /**
     * Get translated message
     *
     * @param key  Message key
     * @param args Message arguments
     * @return Translated message
     */
    public String getMessage(String key, Object... args) {
        String rawMessage = this.messages.getString(key, null);
        if (rawMessage != null) {
            return String.format(rawMessage, args);
        } else if (parent != null) {
            return parent.getMessage(key, args);
        } else {
            return "Message key not found: " + key;
        }
    }

    public ComponentBuilder prefix() {
        return new ComponentBuilder(getMessage("prefix")).color(ChatColor.LIGHT_PURPLE);
    }

    public BaseComponent[] success(String key, Object... args) {
        return prefix().append(new ComponentBuilder(getMessage(key, args)).color(ChatColor.GREEN).create()).create();
    }

    public BaseComponent[] error(String key, Object... args) {
        return prefix().append(new ComponentBuilder(getMessage(key, args)).color(ChatColor.RED).create()).create();
    }

    public BaseComponent[] warning(String key, Object... args) {
        return prefix().append(new ComponentBuilder(getMessage(key, args)).color(ChatColor.YELLOW).create()).create();
    }

    public BaseComponent[] info(String key, Object... args) {
        return prefix().append(new ComponentBuilder(getMessage(key, args)).color(ChatColor.LIGHT_PURPLE).create()).create();
    }
}