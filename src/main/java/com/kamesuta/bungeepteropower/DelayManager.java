package com.kamesuta.bungeepteropower;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Provides a function to stop the server after n seconds
 */
public class DelayManager {
    /**
     * Tasks in progress
     */
    private final ConcurrentMap<String, ScheduledTask> serverStopTasks = new ConcurrentHashMap<>();

    /**
     * Stop the server after a while.
     *
     * @param serverName   The name of the server to stop
     * @param timeout The time in seconds to stop the server
     */
    public void stopAfterWhile(String serverName, int timeout) {
        // Get the Pterodactyl server ID
        String pterodactylServerId = plugin.config.getServerId(serverName);
        if (pterodactylServerId == null) {
            return;
        }

        // Cancel previous task
        cancelStop(serverName);

        // Stop the server after the auto stop time
        AtomicInteger taskId = new AtomicInteger();
        ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            // Log
            logger.info(String.format("Scheduled task executed: stop server %s (task ID: %d)", serverName, taskId.get()));

            // Stop the target server
            PterodactylAPI.sendPowerSignal(serverName, pterodactylServerId, PterodactylAPI.PowerSignal.STOP);

            // Unregister the task
            serverStopTasks.remove(serverName);

        }, timeout, TimeUnit.SECONDS);
        taskId.set(task.getId());

        // Log
        logger.info(String.format("Scheduled task registered: stop server %s (task ID: %d)", serverName, task.getId()));

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
        ScheduledTask task = serverStopTasks.remove(serverName);
        if (task != null) {
            // Log
            logger.info(String.format("Scheduled task canceled: stop server %s (task ID: %d)", serverName, task.getId()));

            task.cancel();
        }
    }
}
