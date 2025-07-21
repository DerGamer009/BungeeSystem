package me.dergamer09.bungeesystem.velocity.listeners;

import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.StatsManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsListener {

    private final VelocitySystem plugin;
    private final StatsManager statsManager;
    
    // Map to track login timestamps for calculating session duration
    private final Map<UUID, Long> loginTimes = new HashMap<>();
    
    public StatsListener(VelocitySystem plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }
    
    /**
     * Handle player login event to record statistics
     */
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        // Record player login in stats
        statsManager.recordLogin(uuid, name);
        
        // Store login time for session tracking
        loginTimes.put(uuid, System.currentTimeMillis());
    }
    
    /**
     * Handle player disconnect event
     */
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check if we have a login time for this player
        if (loginTimes.containsKey(uuid)) {
            long loginTime = loginTimes.get(uuid);
            long currentTime = System.currentTimeMillis();
            long sessionDuration = currentTime - loginTime;
            
            // Update online time in database (assuming we have a method for this)
            updateOnlineTime(uuid, sessionDuration);
            
            // Remove from tracking map
            loginTimes.remove(uuid);
            
            // Clear from cache to ensure fresh data on next login
            statsManager.invalidateCache(uuid);
        }
    }
    
    /**
     * Handle chat messages to record message count in stats
     */
    @Subscribe
    public void onChat(PlayerChatEvent event) {
        // Ignore commands
        if (event.isCommand() || event.isCancelled()) {
            return;
        }
        
        // Make sure sender is a player
        if (event.getPlayer() != null) {
            Player player = event.getPlayer();
            statsManager.recordMessage(player.getUniqueId());
        }
    }
    
    /**
     * Update the player's total online time in the database
     * 
     * @param uuid The UUID of the player
     * @param sessionDuration The duration of the current session in milliseconds
     */
    private void updateOnlineTime(UUID uuid, long sessionDuration) {
        // Execute in async thread to avoid blocking the main thread
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            try {
                // Check if player already has an entry in the online_time table
                boolean exists = false;
                long currentTotal = 0;
                
                java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT total_time FROM online_time WHERE player_uuid = ?"
                );
                ps.setString(1, uuid.toString());
                java.sql.ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    exists = true;
                    currentTotal = rs.getLong("total_time");
                }
                
                rs.close();
                ps.close();
                
                // Add the session duration to the current total
                long newTotal = currentTotal + sessionDuration;
                
                // Update or insert the new total
                if (exists) {
                    ps = conn.prepareStatement(
                        "UPDATE online_time SET total_time = ? WHERE player_uuid = ?"
                    );
                    ps.setLong(1, newTotal);
                    ps.setString(2, uuid.toString());
                } else {
                    ps = conn.prepareStatement(
                        "INSERT INTO online_time (player_uuid, total_time) VALUES (?, ?)"
                    );
                    ps.setString(1, uuid.toString());
                    ps.setLong(2, sessionDuration);
                }
                
                ps.executeUpdate();
                ps.close();
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error updating online time: " + e.getMessage());
                e.printStackTrace();
            }
        }).schedule();
    }
} 