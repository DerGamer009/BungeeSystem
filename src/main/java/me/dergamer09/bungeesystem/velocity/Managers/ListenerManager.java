package me.dergamer09.bungeesystem.velocity.Managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import org.slf4j.Logger;

/**
 * Placeholder listener manager for Velocity.
 */
public class ListenerManager {
    private final VelocitySystem plugin;
    private final Logger logger;

    public ListenerManager(VelocitySystem plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void registerListeners() {
        plugin.getServer().getEventManager().register(plugin, this);
        plugin.getServer().getEventManager().register(plugin, new me.dergamer09.bungeesystem.velocity.listeners.MotdListener(plugin));
    }

    // Example listener for disconnects
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        logger.info(event.getPlayer().getUsername() + " disconnected.");
    }
}
