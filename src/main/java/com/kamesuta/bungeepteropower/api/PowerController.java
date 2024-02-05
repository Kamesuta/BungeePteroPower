package com.kamesuta.bungeepteropower.api;

import java.util.concurrent.CompletableFuture;

/**
 * Power controller interface.
 * <p>
 * We are accepting pull requests for adding built-in power controllers!
 * This is the kind of power controller I want, which can start servers locally,
 * or is compatible with management software other than Pterodactyl,
 * such as “Minecraft Server Manager”, “MCSManager”, or “MC Server Soft”.
 */
public interface PowerController {
    /**
     * Send a power signal to the Pterodactyl server.
     *
     * @param serverName    The name of the server to start
     * @param serverId      The server ID to send the signal to
     * @param signalType    The power signal to send
     * @return A future that completes when the request is finished
     */
    CompletableFuture<Void> sendPowerSignal(String serverName, String serverId, PowerSignal signalType);
}
