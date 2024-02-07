package com.kamesuta.bungeepteropower;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Statistics of the plugin
 */
public class Statistics {
    public final ActionCounter actionCounter = new ActionCounter();
    public final StartReasonRecorder startReasonRecorder = new StartReasonRecorder();

    /**
     * Register bStats
     */
    public void register() {
        // Enable bStats
        Metrics metrics = new Metrics(plugin, 20917);
        ProxyServer proxyServer = ProxyServer.getInstance();
        Set<String> serverNames = plugin.config.getServerNames();

        // Config charts
        metrics.addCustomChart(new SimplePie("powerControllerType", () -> plugin.config.powerControllerType));
        metrics.addCustomChart(new SimplePie("language", () -> plugin.config.language));

        // The number of servers managed by BungeePteroPower
        // I would like to know the percentage of how many servers are using this plugin.
        metrics.addCustomChart(new SimplePie("pteroServerCount", () -> String.valueOf(plugin.config.getServerNames().size())));
        metrics.addCustomChart(new SimplePie("bungeeServerCount", () -> String.valueOf(proxyServer.getServers().size())));

        // The number of power actions performed
        for (ActionCounter.ActionType actionType : ActionCounter.ActionType.values()) {
            metrics.addCustomChart(new SingleLineChart(actionType.name, () -> actionCounter.collect(actionType)));
        }

        // The number of players on the BungeePteroPower-managed servers
        metrics.addCustomChart(new SingleLineChart("pteroPlayerCount", () ->
                serverNames.stream().mapToInt(serverName ->
                        Optional.ofNullable(proxyServer.getServers().get(serverName))
                                .map(serverInfo -> serverInfo.getPlayers().size())
                                .orElse(0)
                ).sum()));

        // Permission plugin
        // Permission settings are always required to use this plugin.
        // Therefore, I would like to know what percentage of servers are using what permission plugin!
        PluginManager pluginManager = proxyServer.getPluginManager();
        metrics.addCustomChart(new SimplePie("permissionPlugin", () -> {
            // Well-known permission plugins (Please let me know if there are any other well-known permission plugins.)
            String[] permissionPlugins = {"LuckPerms", "BungeePerms", "PermissionsCord", "PermissionsEX", "PowerfulPerms", "BungeePexBridge", "UltraPermissions"};
            // Concatenate installed plugins
            String installedPlugins = Stream.of(permissionPlugins).filter(plugin -> pluginManager.getPlugin(plugin) != null).collect(Collectors.joining(","));
            return installedPlugins.isEmpty() ? "None" : installedPlugins;
        }));

        // Percentage of servers that have people on them and by what.
        metrics.addCustomChart(new AdvancedPie("pteroServerStartedBy", () ->
                // Enumerate online servers with players and count the reasons for starting the server
                serverNames.stream()
                        // Get the server info from the plugin's server list
                        .map(serverName -> proxyServer.getServers().get(serverName))
                        .filter(Objects::nonNull)
                        // Filter out servers with no players
                        .filter(serverInfo -> !serverInfo.getPlayers().isEmpty())
                        // Count the reasons for starting the server
                        .map(serverInfo -> startReasonRecorder.get(serverInfo.getName()))
                        .collect(Collectors.groupingBy(startReason -> startReason.name, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)))));
    }

    /**
     * Record the reason for server startup
     */
    public static class StartReasonRecorder {
        private final Map<String, StartReason> reasonMap = new HashMap<>();

        /**
         * Record the reason for server startup
         *
         * @param serverName The name of the server
         * @param reason     The reason for server startup
         */
        public void recordStart(String serverName, StartReason reason) {
            reasonMap.put(serverName, reason);
        }

        /**
         * Record the reason for server stop
         *
         * @param serverName The name of the server
         */
        public void recordStop(String serverName) {
            reasonMap.remove(serverName);
        }

        /**
         * Get the reason for server startup
         *
         * @param serverName The name of the server
         * @return The reason for server startup
         */
        public StartReason get(String serverName) {
            return reasonMap.getOrDefault(serverName, StartReason.OTHER);
        }

        /**
         * Server start reason
         */
        public enum StartReason {
            OTHER("other"),
            COMMAND("command"),
            AUTOJOIN("autojoin"),
            ;

            public final String name;

            StartReason(String name) {
                this.name = name;
            }
        }
    }

    /**
     * The counter for each action
     */
    public static class ActionCounter {
        private final Map<ActionType, AtomicInteger> countMap = new EnumMap<>(ActionType.class);

        /**
         * Increment the counter
         *
         * @param actionType type of action to get statistics for
         */
        public void increment(ActionType actionType) {
            getOrCreate(actionType).incrementAndGet();
        }

        /**
         * Get the collected value and reset the counter
         *
         * @param actionType type of action to get statistics for
         * @return The collected value
         */
        public int collect(ActionType actionType) {
            return getOrCreate(actionType).getAndSet(0);
        }

        /**
         * Get or create the counter
         *
         * @param actionType type of action to get statistics for
         * @return The counter
         */
        private AtomicInteger getOrCreate(ActionType actionType) {
            return countMap.computeIfAbsent(actionType, (k) -> new AtomicInteger());
        }

        /**
         * The service to collect statistics
         */
        public enum ActionType {
            START_SERVER_COMMAND("startServerByCommand"),
            STOP_SERVER_COMMAND("stopServerByCommand"),
            START_SERVER_AUTOJOIN("startServerByAutoJoin"),
            STOP_SERVER_NOBODY("stopServerByNobody"),
            ;

            public final String name;

            ActionType(String name) {
                this.name = name;
            }
        }
    }
}
