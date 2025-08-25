package me.dergamer09.bungeesystem.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Handles player events for Velocity and sends them to the API
 */
public class PlayerEventListener {
    
    private final VelocitySystem plugin;
    private final Map<String, Long> playerJoinTimes = new ConcurrentHashMap<>();
    
    public PlayerEventListener(VelocitySystem plugin) {
        this.plugin = plugin;
    }
    
    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        Player player = event.getPlayer();
        
        // Record join time for session duration calculation
        playerJoinTimes.put(player.getUniqueId().toString(), System.currentTimeMillis());

    }
    
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        
        // Calculate session duration
        long sessionDuration = 0;
        if (playerJoinTimes.containsKey(uuid)) {
            long joinTime = playerJoinTimes.remove(uuid);
            sessionDuration = (System.currentTimeMillis() - joinTime) / 1000; // Convert to seconds
        }
        
        // Get current server
        String serverName = "unknown";
        if (player.getCurrentServer().isPresent()) {
            serverName = player.getCurrentServer().get().getServerInfo().getName();
        }

    }
    
    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        RegisteredServer toServer = event.getServer();
        
        // Get previous server if available
        String fromServerName = "unknown";
        if (event.getPreviousServer().isPresent()) {
            fromServerName = event.getPreviousServer().get().getServerInfo().getName();
        }
        
        String toServerName = toServer.getServerInfo().getName();
        
        // Only send if this is actually a server switch (not initial connect)
        if (event.getPreviousServer().isPresent()) {
            // Server switch tracking can be added here if needed
        }
    }
}