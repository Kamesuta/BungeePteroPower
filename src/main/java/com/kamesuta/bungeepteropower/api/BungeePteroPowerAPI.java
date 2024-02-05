package com.kamesuta.bungeepteropower.api;

import com.kamesuta.bungeepteropower.BungeePteroPower;

/**
 * API for BungeePteroPower.
 * You can use this API to register a custom power controller. (e.g. for a custom server hosting service)
 */
public interface BungeePteroPowerAPI {
    /**
     * Get the API instance.
     * You need to add BungeePteroPower to your dependencies to use this method.
     *
     * @return The API instance, or null if BungeePteroPower is not installed
     */
    static BungeePteroPowerAPI getInstance() {
        return BungeePteroPower.plugin;
    }

    /**
     * Register a power controller.
     * You can use this method to add a custom power controller.
     *
     * @param name       The name of the power controller
     *                   (e.g. "pterodactyl" for the built-in Pterodactyl power controller)
     *                   You can use this name to specify the power controller in the configuration.
     *                   (e.g. "power-controller: pterodactyl")
     * @param controller Your power controller
     */
    void registerPowerController(String name, PowerController controller);

    /**
     * Unregister a power controller.
     *
     * @param name The name of the power controller to unregister
     */
    void unregisterPowerController(String name);
}
