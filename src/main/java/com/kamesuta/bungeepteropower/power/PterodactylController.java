package com.kamesuta.bungeepteropower.power;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kamesuta.bungeepteropower.ServerController;
import com.kamesuta.bungeepteropower.api.PowerController;
import com.kamesuta.bungeepteropower.api.PowerSignal;
import com.kamesuta.bungeepteropower.api.PowerStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
        return ServerController.waitUntil(serverName, serverId, PowerStatus.OFFLINE)
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
     * Check the power status of the server.
     *
     * @param serverName The name of the server to check
     * @param serverId   The server ID to check
     * @return A future that completes with the power status of the server
     */
    @Override
    public CompletableFuture<PowerStatus> checkPowerStatus(String serverName, String serverId) {
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
                        String state = root.getAsJsonObject("attributes").get("current_state").getAsString();
                        try {
                            return PowerStatus.valueOf(state.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            logger.warning("Unknown server state: " + state + " for server: " + serverName);
                            return PowerStatus.OFFLINE;
                        }
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
                });
    }
}
