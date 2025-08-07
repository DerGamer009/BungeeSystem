package me.dergamer09.bungeesystem.listeners;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import me.dergamer09.bungeesystem.commands.AfkCommand;
import me.dergamer09.bungeesystem.commands.MaintenanceCommand;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerEventListener implements Listener {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;

    public PlayerEventListener() {
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(PreLoginEvent event) {
        // We can't check UUID whitelist at this stage, so we'll check in PostLoginEvent
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Check for maintenance mode
        if (MaintenanceCommand.isMaintenanceMode()) {
            if (!player.hasPermission("bungeesystem.maintenance.bypass") && !configManager.isInWhitelist(uuid)) {
                // Player is not whitelisted, disconnect them
                player.disconnect(new TextComponent(
                        configManager.getMessage("join.maintenance_kick") + "\n" +
                        configManager.getMessage("join.maintenance_kick_info")
                ));
                return;
            }
        }

        // Track online time in database
        try {
            // Update online_time table
            PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(
                    "INSERT INTO online_time (player_uuid, total_time, last_login) VALUES (?, 0, ?) " +
                            "ON DUPLICATE KEY UPDATE last_login = ?");
            ps.setString(1, uuid.toString());
            ps.setLong(2, now);
            ps.setLong(3, now);
            ps.executeUpdate();
            ps.close();
            
            // Update or create player_data entry for seen/whois commands
            updatePlayerData(player, now);
            
            // Send player join event to API
            if (plugin.getApiManager().isApiEnabled()) {
                String serverName = player.getServer() != null ? player.getServer().getInfo().getName() : "unknown";
                plugin.getApiManager().sendPlayerJoin(player.getName(), uuid.toString(), serverName);
                
                // Check for bans via API
                if (plugin.getConfig().getBoolean("features.punishment_checks", true)) {
                    if (plugin.getApiManager().checkPlayerBan(player.getName(), uuid.toString())) {
                        player.disconnect(new TextComponent(
                                configManager.getMessage("punishment.ban_kick_message",
                                        "reason", "You are banned from this server",
                                        "expire", "Permanent")));
                        return;
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        try {
            // Update online_time
            PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(
                    "UPDATE online_time SET total_time = total_time + (? - last_login), last_login = 0 WHERE player_uuid = ?");
            ps.setLong(1, now);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
            ps.close();
            
            // Update last_seen time
            updateLastSeen(uuid, now);
            
            // Clear AFK status
            AfkCommand.clearAfk(uuid);

            // Cleanup chat state
            plugin.getChatManager().handlePlayerDisconnect(uuid);

            // Remove messaging data to avoid memory leaks
            BungeeSystem.lastMessageMap.remove(uuid);
            BungeeSystem.lastMessageMap.entrySet().removeIf(e -> uuid.equals(e.getValue()));
            BungeeSystem.ignoredPlayers.remove(uuid);

            // Send player quit event to API
            if (plugin.getApiManager().isApiEnabled()) {
                String serverName = player.getServer() != null ? player.getServer().getInfo().getName() : "unknown";
                // Calculate session duration (simplified - could be more accurate)
                long sessionDuration = 1800; // Default 30 minutes, could be calculated from login time
                plugin.getApiManager().sendPlayerQuit(player.getName(), uuid.toString(), serverName, sessionDuration);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onServerSwitch(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Only track server switches (not initial connections)
        if (player.getServer() != null && event.getTarget() != null) {
            String fromServer = player.getServer().getInfo().getName();
            String toServer = event.getTarget().getName();
            
            // Send player switch event to API
            if (plugin.getApiManager().isApiEnabled()) {
                plugin.getApiManager().sendPlayerSwitch(player.getName(), uuid.toString(), fromServer, toServer);
            }
        }
    }
    
    /**
     * Update or create player data entry for seen/whois commands
     * 
     * @param player The player to update
     * @param loginTime The login timestamp
     * @throws SQLException If a database error occurs
     */
    private void updatePlayerData(ProxiedPlayer player, long loginTime) throws SQLException {
        Connection conn = plugin.getDatabaseManager().getConnection();
        
        // Create player_data table if it doesn't exist
        PreparedStatement createTable = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "name VARCHAR(16) NOT NULL, " +
                        "ip VARCHAR(45), " +
                        "first_join BIGINT, " +
                        "last_seen BIGINT" +
                        ")"
        );
        createTable.executeUpdate();
        createTable.close();
        
        // Check if player already exists
        PreparedStatement checkPs = conn.prepareStatement(
                "SELECT first_join FROM player_data WHERE uuid = ? LIMIT 1");
        checkPs.setString(1, player.getUniqueId().toString());
        ResultSet rs = checkPs.executeQuery();
        
        if (rs.next()) {
            // Player exists, update name and IP
            PreparedStatement updatePs = conn.prepareStatement(
                    "UPDATE player_data SET name = ?, ip = ? WHERE uuid = ?");
            updatePs.setString(1, player.getName());
            updatePs.setString(2, player.getSocketAddress().toString().replace("/", "").split(":")[0]);
            updatePs.setString(3, player.getUniqueId().toString());
            updatePs.executeUpdate();
            updatePs.close();
        } else {
            // New player, insert data
            PreparedStatement insertPs = conn.prepareStatement(
                    "INSERT INTO player_data (uuid, name, ip, first_join, last_seen) VALUES (?, ?, ?, ?, ?)");
            insertPs.setString(1, player.getUniqueId().toString());
            insertPs.setString(2, player.getName());
            insertPs.setString(3, player.getSocketAddress().toString().replace("/", "").split(":")[0]);
            insertPs.setLong(4, loginTime);
            insertPs.setLong(5, loginTime);
            insertPs.executeUpdate();
            insertPs.close();
        }
        
        rs.close();
        checkPs.close();
    }
    
    /**
     * Update a player's last seen time
     * 
     * @param uuid The UUID of the player
     * @param time The timestamp to set
     * @throws SQLException If a database error occurs
     */
    private void updateLastSeen(UUID uuid, long time) throws SQLException {
        Connection conn = plugin.getDatabaseManager().getConnection();
        
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE player_data SET last_seen = ? WHERE uuid = ?");
        ps.setLong(1, time);
        ps.setString(2, uuid.toString());
        ps.executeUpdate();
        ps.close();
    }
}
