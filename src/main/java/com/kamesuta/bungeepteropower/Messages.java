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
import java.nio.file.Files;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Plugin Translations
 */
public class Messages {
    private final Configuration messages;

    /**
     * Load messages_jp.yml
     *
     * @param language Language
     * @return Messages
     */
    public Messages(String language) {
        try {
            // Create the data folder if it does not exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdir();
            }

            File file = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
            if (!file.exists()) {
                try (InputStream in = plugin.getResourceAsStream("messages_" + language + ".yml")) {
                    Files.copy(in, file.toPath());
                }
            }

            // Load messages_jp.yml
            this.messages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            logger.severe("Failed to create/load messages_jp.yml");
            throw new RuntimeException(e);
        }
    }

    /**
     * Get translated message
     *
     * @param key Message key
     * @param args Message arguments
     * @return Translated message
     */
    public String getMessage(String key, Object... args) {
        return String.format(this.messages.getString(key), args);
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

    public BaseComponent[] usage(String key, Object... args) {
        return prefix().append(new ComponentBuilder(getMessage(key, args)).color(ChatColor.YELLOW).create()).create();
    }

    public BaseComponent[] info(String key, Object... args) {
        return prefix().append(new ComponentBuilder(getMessage(key, args)).color(ChatColor.LIGHT_PURPLE).create()).create();
    }
}