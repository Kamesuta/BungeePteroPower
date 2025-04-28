package com.kamesuta.bungeepteropower.power;

import com.kamesuta.bungeepteropower.api.PowerController;
import com.kamesuta.bungeepteropower.api.PowerSignal;

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
 * Crafty API client.
 */
public class CraftyController implements PowerController {
    /**
     * Send a power signal to the Crafty server.
     *
     * @param serverName The name of the server to start
     * @param serverId   The Crafty server ID
     * @param signalType The power signal to send
     * @return A future that completes when the request is finished
     */
    @Override
    public CompletableFuture<Void> sendPowerSignal(String serverName, String serverId, PowerSignal signalType) {
        String signal = signalType.getSignal();
        String action = signalType == PowerSignal.START ? "start_server" : "stop_server";
        logger.info(String.format("%s server: %s (Crafty server ID: %s)", action, serverName, serverId));

        // Create a path
        String path = "/api/v2/servers/" + serverId + "/action/" + action;

        // Create a request
        HttpRequest request = requestBuilder(path)
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(status -> {
                    int code = status.statusCode();
                    logger.info(status.toString());
                    if (code == 200) {
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
                    logger.log(Level.WARNING, "Failed to send " + action + " signal to the server: " + serverName, e);
                    throw new CompletionException(e);
                });
    }

    /**
     * Restore from a backup.
     * Send a stop signal to the server, wait until the server is offline, and then
     * restore from a backup.
     *
     * @param serverName The name of the server
     * @param serverId   The Crafty server ID
     * @param backupName The file name of the backup
     * @return A future that completes when the request to restore from the backup is sent after the server becomes offline
     */
    @Override
    public CompletableFuture<Void> sendRestoreSignal(String serverName, String serverId, String backupName) {
        throw new UnsupportedOperationException(
                "Feature incomplete at this time. The Crafty 4 Controller API doesn't provide restore function.");
    }

    /**
     * Create a request builder with custom headers
     *
     * @param The path to the request
     * @return A request builder with custom headers
     */
    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(plugin.config.craftyUrl.resolve(path).toString()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + plugin.config.craftyApiKey);
        plugin.config.customHeaders.forEach(builder::header);
        return builder;
    }
}
