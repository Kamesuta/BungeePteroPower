package com.kamesuta.bungeepteropower.api;

/**
 * Represents the power status of a server.
 */
public enum PowerStatus {
    /**
     * The server is offline.
     */
    OFFLINE,
    /**
     * The server is starting up.
     */
    STARTING,
    /**
     * The server is running and ready to accept connections.
     */
    RUNNING,
    /**
     * The server is stopping.
     */
    STOPPING
} 