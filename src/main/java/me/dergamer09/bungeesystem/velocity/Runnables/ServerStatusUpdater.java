package me.dergamer09.bungeesystem.velocity.runnables;

import me.dergamer09.bungeesystem.velocity.VelocitySystem;

/**
 * Updates server status periodically to the API
 */
public class ServerStatusUpdater implements Runnable {
    
    private final VelocitySystem plugin;
    
    public ServerStatusUpdater(VelocitySystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        if (!plugin.getApiManager().isApiEnabled()) {
            return;
        }
        
        try {
            // Collect server statistics
            int onlinePlayers = plugin.getServer().getPlayerCount();
            int maxPlayers = 100; // Default, could be configurable
            
            // TPS calculation for Velocity (simplified)
            double tps = 20.0; // Velocity doesn't have direct TPS access like BungeeCord
            
            // System statistics
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double ramUsage = (double) usedMemory / maxMemory * 100.0;
            
            // CPU usage (simplified - would need more complex implementation for accurate CPU)
            double cpuUsage = Math.min(ramUsage * 0.8, 100.0); // Rough estimate
            
            // Server status
            String status = "online";
            
            // Send to API
            plugin.getApiManager().sendServerStatus(
                onlinePlayers,
                maxPlayers,
                tps,
                cpuUsage,
                ramUsage,
                status
            );
            
        } catch (Exception e) {
            plugin.getLogger().warn("Error updating server status: " + e.getMessage());
        }
    }
}
