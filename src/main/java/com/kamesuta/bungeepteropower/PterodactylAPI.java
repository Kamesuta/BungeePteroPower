package com.kamesuta.bungeepteropower;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;
import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * Pterodactyl API client.
 */
public class PterodactylAPI {
    /**
     * Send a power signal to the Pterodactyl server.
     *
     * @param serverName          The name of the server to start
     * @param pterodactylServerId The Pterodactyl server ID
     * @param signal              The power signal to send
     * @return A future that completes when the request is finished
     */
    public static CompletableFuture<Void> sendPowerSignal(String serverName, String pterodactylServerId, PowerSignal signal) {
        String doing = signal == PowerSignal.START ? "Starting" : "Stopping";
        logger.info(String.format("%s server: %s (Pterodactyl server ID: %s)", doing, serverName, pterodactylServerId));

        // Create a path
        String path = "/api/client/servers/" + pterodactylServerId + "/power";

        HttpClient client = HttpClient.newHttpClient();

        // Create a form body to send power signal
        String formBody = "signal=" + signal.signal;

        // Create a request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(plugin.config.pterodactylUrl.resolve(path).toString()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + plugin.config.pterodactylToken)
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        // Execute request and register a callback
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(code -> {
                    if (code >= 200 && code < 300) {
                        logger.info("Successfully " + signal.signal + " server: " + serverName);
                        future.complete(null);
                    } else {
                        String message = "Failed to " + signal.signal + " server: " + serverName + ". Response code: " + code;
                        logger.warning(message);
                        future.completeExceptionally(new RuntimeException(message));
                    }
                })
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Failed to " + signal.signal + " server: " + serverName, e);
                    future.completeExceptionally(e);
                    return null;
                });

        return future;
    }

    /**
     * Power signal type.
     */
    public enum PowerSignal {
        START("start"),
        STOP("stop"),
        ;

        /**
         * The signal string to send to the Pterodactyl server.
         */
        public final String signal;

        PowerSignal(String signal) {
            this.signal = signal;
        }
    }
}
