package com.kamesuta.bungeepteropower.api;

import java.util.concurrent.CompletableFuture;

/**
 * Power controller interface.
 * <p>
 * We are accepting pull requests for adding built-in power controllers!
 * This is the kind of power controller I want, which can start servers locally,
 * or is compatible with management software other than Pterodactyl,
 * such as "Minecraft Server Manager", "MCSManager", or "MC Server Soft".
 */
public interface PowerController {
    /**
     * Send a power signal to the Pterodactyl server.
     *
     * @param serverName The name of the server to start
     * @param serverId   The server ID to send the signal to
     * @param signalType The power signal to send
     * @return A future that completes when the request is finished
     */
    CompletableFuture<Void> sendPowerSignal(String serverName, String serverId, PowerSignal signalType);

    /**
     * Check the power status of the server.
     *
     * @param serverName The name of the server to check
     * @param serverId   The server ID to check
     * @return A future that completes with the power status of the server
     */
    default CompletableFuture<PowerStatus> checkPowerStatus(String serverName, String serverId) {
        throw new UnsupportedOperationException("This power controller does not support checking offline status.");
    }

    /**
     * Restore from a backup.
     * Send a stop signal to the server, wait until the server is offline, and then restore from a backup.
     *
     * @param serverName The name of the server
     * @param serverId   The server ID
     * @param backupName The name of the backup
     * @return A future that completes when the request to restore from the backup is sent after the server becomes offline
     */
    default CompletableFuture<Void> sendRestoreSignal(String serverName, String serverId, String backupName) {
        throw new UnsupportedOperationException("This power controller does not support restore signal.");
    }
}
