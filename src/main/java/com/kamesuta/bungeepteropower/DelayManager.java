package com.kamesuta.bungeepteropower;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Provides a function to stop the server after n seconds
 */
public class DelayManager {
    /**
     * Tasks in progress
     */
    private final Map<String, ScheduledTask> serverStopTasks = new HashMap<>();

    /**
     * Stop the server after a while.
     *
     * @param serverName   The name of the server to stop
     * @param autoStopTime The time in seconds to stop the server
     */
    public void stopAfterWhile(String serverName, int autoStopTime) {
        // Log
        logger.info(String.format("Scheduled to stop server %s after %d seconds", serverName, autoStopTime));

        // Get the Pterodactyl server ID
        String pterodactylServerId = plugin.config.getServerId(serverName);
        if (pterodactylServerId == null) {
            return;
        }

        // Cancel previous task
        cancelStop(serverName);

        // Stop the server after the auto stop time
        ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            // Stop the target server
            PterodactylAPI.sendPowerSignal(serverName, pterodactylServerId, PterodactylAPI.PowerSignal.STOP);
        }, autoStopTime, TimeUnit.SECONDS);

        // Register the task
        serverStopTasks.put(serverName, task);
    }

    /**
     * Cancel the task to stop the server.
     *
     * @param serverName The name of the server to cancel stopping
     */
    public void cancelStop(String serverName) {
        // Cancel the task
        ScheduledTask task = serverStopTasks.get(serverName);
        if (task != null) {
            // Log
            logger.info(String.format("Canceled to stop server %s", serverName));

            task.cancel();
        }
    }
}
