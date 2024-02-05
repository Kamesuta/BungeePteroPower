package com.kamesuta.bungeepteropower.api;

/**
 * Power signal type.
 */
public enum PowerSignal {
    /**
     * Start the server
     */
    START,
    /**
     * Stop the server
     */
    STOP,
    ;

    /**
     * Get the signal string.
     * It is used in the language file and Pterodactyl API.
     *
     * @return The signal string
     */
    public String getSignal() {
        return name().toLowerCase();
    }
}