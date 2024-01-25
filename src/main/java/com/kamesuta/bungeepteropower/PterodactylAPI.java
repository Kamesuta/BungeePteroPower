package com.kamesuta.bungeepteropower;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;

/**
 * Pterodactyl API client.
 */
public class PterodactylAPI {
    private final URI pterodactylUrl;
    private final String pterodactylToken;

    /**
     * Create a new Pterodactyl API client.
     *
     * @param pterodactylUrl   The Pterodactyl server url
     * @param pterodactylToken The Pterodactyl API token
     */
    public PterodactylAPI(URI pterodactylUrl, String pterodactylToken) {
        this.pterodactylUrl = pterodactylUrl;
        this.pterodactylToken = pterodactylToken;
    }

    /**
     * Send a power signal to the Pterodactyl server.
     *
     * @param serverName          The name of the server to start
     * @param pterodactylServerId The Pterodactyl server ID
     * @param signal              The power signal to send
     * @return A future that completes when the request is finished
     */
    public CompletableFuture<Void> sendPowerSignal(String serverName, String pterodactylServerId, PowerSignal signal) {
        String doing = signal == PowerSignal.START ? "Starting" : "Stopping";
        logger.info(String.format("%s server: %s (Pterodactyl server ID: %s)", doing, serverName, pterodactylServerId));

        // Create a path
        String path = "/api/client/servers/" + pterodactylServerId + "/power";

        OkHttpClient client = new OkHttpClient();

        // Create a form body to send power signal
        FormBody.Builder formBuilder = new FormBody.Builder();
        formBuilder.add("signal", signal.signal);
        RequestBody formBody = formBuilder.build();

        // Create a request
        Request request = new Request.Builder()
                .url(pterodactylUrl.resolve(path).toString())
                .post(formBody)
                .addHeader("Authorization", "Bearer " + pterodactylToken)
                .build();

        // Execute request and register a callback
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.log(Level.WARNING, "Failed to " + signal.signal + " server: " + serverName, e);
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    logger.info("Successfully " + signal.signal + " server: " + serverName);
                    future.complete(null);
                } else {
                    String message = "Failed to " + signal.signal + " server: " + serverName + ". Response: " + response;
                    logger.warning(message);
                    future.completeExceptionally(new RuntimeException(message));
                }
                response.close();
            }
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
