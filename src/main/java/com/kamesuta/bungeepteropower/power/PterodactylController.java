package com.kamesuta.bungeepteropower.power;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kamesuta.bungeepteropower.api.PowerController;
import com.kamesuta.bungeepteropower.api.PowerSignal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Pterodactyl API client.
 */
public class PterodactylController implements PowerController {
    /**
     * Send a power signal to the Pterodactyl server.
     *
     * @param serverName The name of the server to start
     * @param serverId   The Pterodactyl server ID
     * @param signalType The power signal to send
     * @return A future that completes when the request is finished
     */
    @Override
    public CompletableFuture<Void> sendPowerSignal(String serverName, String serverId, PowerSignal signalType) {
        String signal = signalType.getSignal();
        String doing = signalType == PowerSignal.START ? "Starting" : "Stopping";
        logger.info(String.format("%s server: %s (Pterodactyl server ID: %s)", doing, serverName, serverId));

        // Create a path
        String path = "/api/client/servers/" + serverId + "/power";

        // Create a JSON body to send power signal
        String jsonBody = "{\"signal\": \"" + signal + "\"}";

        // Create a request
        HttpRequest request = requestBuilder(path)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Execute request and register a callback
        return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(status -> {
                    int code = status.statusCode();
                    if (code == 204) {
                        logger.info("Successfully sent " + signal + " signal to the server: " + serverName);
                        return (Void) null;
                    } else {
                        String message = "Failed to send " + signal + " signal to the server: " + serverName + ". Response code: " + code;
                        logger.warning(message);
                        logger.info("Request: " + request + ", Response: " + code + " " + status.body());
                        throw new RuntimeException(message);
                    }
                })
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Failed to send " + signal + " signal to the server: " + serverName, e);
                    throw new CompletionException(e);
                });
    }

    /**
     * Restore from a backup.
     * Send a stop signal to the server, wait until the server is offline, and then restore from a backup.
     *
     * @param serverName The name of the server
     * @param serverId   The Pterodactyl server ID
     * @param backupName The name of the backup
     * @return A future that completes when the request to restore from the backup is sent after the server becomes offline
     */
    @Override
    public CompletableFuture<Void> sendRestoreSignal(String serverName, String serverId, String backupName) {
        // First, stop the server
        sendPowerSignal(serverName, serverId, PowerSignal.STOP);

        // Wait until the power status becomes offline
        logger.info(String.format("Waiting server to stop: %s (Pterodactyl server ID: %s)", serverName, serverId));
        return waitUntilOffline(serverName, serverId)
                .thenCompose((v) -> {
                    // Restore the backup
                    logger.info(String.format("Successfully stopped server: %s", serverName));
                    return restoreBackup(serverName, serverId, backupName);
                });
    }

    /**
     * Create a request builder with custom headers
     *
     * @param path The path to the request
     * @return A request builder with custom headers
     */
    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(plugin.config.pterodactylUrl.resolve(path).toString()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + plugin.config.pterodactylApiKey);
        plugin.config.customHeaders.forEach(builder::header);
        return builder;
    }

    /**
     * Restore from a backup.
     *
     * @param serverName The name of the server
     * @param serverId   The Pterodactyl server ID
     * @param backupUuid The UUID of the backup
     * @return A future that completes when the request is finished
     */
    private CompletableFuture<Void> restoreBackup(String serverName, String serverId, String backupUuid) {
        logger.info(String.format("Restoring from backup: %s to server: %s (Pterodactyl server ID: %s)", backupUuid, serverName, serverId));

        // Create a path
        String path = "/api/client/servers/" + serverId + "/backups/" + backupUuid + "/restore";

        // Create a JSON body to delete all files
        String jsonBody = "{\"truncate\":true}";

        // Create a request
        HttpRequest request = requestBuilder(path)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Execute request and register a callback
        return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(status -> {
                    int code = status.statusCode();
                    if (code == 204) {
                        logger.info("Successfully restored backup: " + backupUuid + " to server: " + serverName);
                        return (Void) null;
                    } else {
                        String message = "Failed to restore backup: " + backupUuid + " to server: " + serverName + ". Response code: " + code;
                        logger.warning(message);
                        logger.info("Request: " + request + ", Response: " + code + " " + status.body());
                        throw new RuntimeException(message);
                    }
                })
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Failed to restore backup: " + backupUuid + " to server: " + serverName, e);
                    throw new CompletionException(e);
                });
    }

    /**
     * Wait until the power status becomes offline.
     *
     * @param serverName The name of the server
     * @param serverId   The Pterodactyl server ID
     * @return A future that waits until the server becomes offline
     */
    private CompletableFuture<Void> waitUntilOffline(String serverName, String serverId) {
        CompletableFuture<Void> future = new CompletableFuture<Void>().orTimeout(plugin.config.restoreTimeout, TimeUnit.SECONDS);
        // Wait until the server becomes offline
        Consumer<Boolean> callback = new Consumer<>() {
            @Override
            public void accept(Boolean isOffline) {
                // Do nothing if timeout or already completed
                if (future.isDone()) {
                    return;
                }
                // Complete if the server is offline
                if (isOffline) {
                    future.complete(null);
                    return;
                }
                // Otherwise schedule another ping
                logger.fine("Server is still online. Waiting for it to be offline: " + serverName);
                plugin.getProxy().getScheduler().schedule(plugin, () -> checkOffline(serverName, serverId).thenAccept(this), plugin.config.restorePingInterval, TimeUnit.SECONDS);
            }
        };
        // Initial check
        checkOffline(serverName, serverId).thenAccept(callback);

        return future;
    }

    /**
     * Check if the server is offline.
     *
     * @param serverName The name of the server to check
     * @param serverId   The server ID to check
     * @return A future that completes with true if the server is offline, false otherwise
     */
    public CompletableFuture<Boolean> checkOffline(String serverName, String serverId) {
        // Create a path
        String path = "/api/client/servers/" + serverId + "/resources";

        // Create a request
        HttpRequest request = requestBuilder(path)
                .GET()
                .build();

        // Execute request and register a callback
        return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(status -> {
                    int code = status.statusCode();
                    if (code == 200) {
                        // Parse JSON (attributes.current_state)
                        JsonObject root = JsonParser.parseString(status.body()).getAsJsonObject();
                        return root.getAsJsonObject("attributes").get("current_state").getAsString();
                    } else {
                        String message = "Failed to get power status of server: " + serverName + ". Response code: " + code;
                        logger.warning(message);
                        logger.info("Request: " + request + ", Response: " + code + " " + status.body());
                        throw new RuntimeException(message);
                    }
                })
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Failed to get power status of server: " + serverName, e);
                    throw new CompletionException(e);
                })
                .thenApply(powerStatus -> powerStatus.equals("offline"));
    }
}
