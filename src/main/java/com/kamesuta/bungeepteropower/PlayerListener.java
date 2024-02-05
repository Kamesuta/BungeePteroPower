package com.kamesuta.bungeepteropower;

import com.kamesuta.bungeepteropower.api.PowerSignal;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import static com.kamesuta.bungeepteropower.BungeePteroPower.plugin;

/**
 * The event listener.
 * Listens player events for auto start and stop the server.
 */
public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ProxyServer instance = ProxyServer.getInstance();

        // Call permission check for the all servers to register the permission to LuckPerms
        for (ServerInfo server : instance.getServers().values()) {
            player.hasPermission("ptero.autostart." + server.getName());
            player.hasPermission("ptero.start." + server.getName());
            player.hasPermission("ptero.stop." + server.getName());
        }
        player.hasPermission("ptero.reload");
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        // Get the target server
        ServerInfo targetServer = event.getTarget();
        ProxiedPlayer player = event.getPlayer();
        ProxyServer instance = ProxyServer.getInstance();

        // Cancel the task to stop the server
        plugin.delay.cancelStop(targetServer.getName());

        // Permission check
        boolean autostart = player.hasPermission("ptero.autostart." + targetServer.getName());
        boolean start = player.hasPermission("ptero.start." + targetServer.getName());
        if (!autostart && !start) {
            return;
        }

        // If anyone is connected to the target server, nothing needs to be done
        if (!targetServer.getPlayers().isEmpty()) {
            return;
        }

        // Get the Pterodactyl server ID
        String serverId = plugin.config.getServerId(targetServer.getName());
        if (serverId == null) {
            return;
        }

        // Ping the target server and check if it is offline
        targetServer.ping((result, error) -> {
            if (error != null) { // The server is offline
                String serverName = targetServer.getName();

                // Start the target server
                if (autostart) {
                    // Send title and message
                    player.sendTitle(instance.createTitle()
                            .title(new ComponentBuilder(plugin.messages.getMessage("join_autostart_title", serverName)).color(ChatColor.YELLOW).create())
                            .subTitle(new ComponentBuilder(plugin.messages.getMessage("join_autostart_subtitle", serverName)).create())
                    );

                    // Send power signal
                    ServerController.sendPowerSignal(player, serverName, serverId, PowerSignal.START);

                } else {
                    // Send message including the command to start the server
                    player.sendMessage(plugin.messages.warning("join_start", serverName));
                    player.sendMessage(new ComponentBuilder()
                            .append(plugin.messages.success("join_start_button", serverName))
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ptero start " + serverName))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.messages.getMessage("join_start_button_tooltip", serverName))))
                            .color(ChatColor.GREEN)
                            .create());

                }
            }
        });
    }

    @EventHandler(priority = (byte) 1024)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // Called when a player disconnect from proxy IN the target server
        ServerInfo targetServer = event.getPlayer().getServer().getInfo();

        onPlayerQuit(event.getPlayer(), targetServer);
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        // Called when a player switch the server
        ServerInfo targetServer = event.getFrom();
        if (targetServer == null) {
            // Called when a player join the proxy
            return;
        }

        onPlayerQuit(event.getPlayer(), targetServer);
    }

    /**
     * Called when a player quits the target server.
     *
     * @param player       The player who quit the target server
     * @param targetServer The target server
     */
    private void onPlayerQuit(ProxiedPlayer player, ServerInfo targetServer) {
        // If you are last player on the target server, stop the server after a while
        // Check if the server is empty, or only you are on the server
        if (!(targetServer.getPlayers().isEmpty()
                || targetServer.getPlayers().size() == 1 && targetServer.getPlayers().contains(player))) {
            return;
        }

        // Get the auto stop time
        Integer serverTimeout = plugin.config.getServerTimeout(targetServer.getName());
        if (serverTimeout != null && serverTimeout >= 0) {
            // Stop the server after a while
            plugin.delay.stopAfterWhile(targetServer.getName(), serverTimeout);
            // Send message
            player.sendMessage(plugin.messages.info("leave_server_stopping", targetServer.getName(), serverTimeout));
        }
    }

}
