package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages player statistics tracking and retrieval
 */
public class StatsManager {

    private final BungeeSystem plugin;
    
    // Cache for player stats to reduce database calls
    private final Map<UUID, PlayerStats> statsCache = new HashMap<>();
    
    public StatsManager(BungeeSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Set up the necessary database tables for player statistics
     */
    public void setupTables() {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Create player_stats table if it doesn't exist
            PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16) NOT NULL, " +
                "first_join BIGINT, " +
                "last_join BIGINT, " +
                "login_count INT DEFAULT 0, " +
                "votes INT DEFAULT 0, " +
                "messages_sent INT DEFAULT 0" +
                ")"
            );
            ps.executeUpdate();
            ps.close();
            
            plugin.getLogger().info("Statistics tables have been set up.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error setting up stats tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Record a player login
     * 
     * @param uuid The UUID of the player
     * @param name The name of the player
     */
    public void recordLogin(UUID uuid, String name) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                long now = System.currentTimeMillis();
                
                // Check if player exists in stats table
                PreparedStatement check = conn.prepareStatement(
                    "SELECT first_join FROM player_stats WHERE uuid = ?"
                );
                check.setString(1, uuid.toString());
                ResultSet rs = check.executeQuery();
                
                if (rs.next()) {
                    // Player exists, update login data
                    PreparedStatement update = conn.prepareStatement(
                        "UPDATE player_stats SET name = ?, last_join = ?, login_count = login_count + 1 WHERE uuid = ?"
                    );
                    update.setString(1, name);
                    update.setLong(2, now);
                    update.setString(3, uuid.toString());
                    update.executeUpdate();
                    update.close();
                } else {
                    // New player, insert data
                    PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO player_stats (uuid, name, first_join, last_join, login_count) VALUES (?, ?, ?, ?, 1)"
                    );
                    insert.setString(1, uuid.toString());
                    insert.setString(2, name);
                    insert.setLong(3, now);
                    insert.setLong(4, now);
                    insert.executeUpdate();
                    insert.close();
                }
                
                rs.close();
                check.close();
                
                // Update cache
                invalidateCache(uuid);
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Error recording player login: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Record a player vote
     * 
     * @param uuid The UUID of the player
     */
    public void recordVote(UUID uuid) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE player_stats SET votes = votes + 1 WHERE uuid = ?"
                );
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                ps.close();
                
                // Update cache
                invalidateCache(uuid);
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Error recording player vote: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Record a player message
     * 
     * @param uuid The UUID of the player
     */
    public void recordMessage(UUID uuid) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE player_stats SET messages_sent = messages_sent + 1 WHERE uuid = ?"
                );
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                ps.close();
                
                // Update cache
                invalidateCache(uuid);
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Error recording player message: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Get player statistics
     * 
     * @param uuid The UUID of the player
     * @return A PlayerStats object containing the player's statistics, or null if not found
     */
    public PlayerStats getPlayerStats(UUID uuid) {
        // Check cache first
        if (statsCache.containsKey(uuid)) {
            return statsCache.get(uuid);
        }
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // First, get basic stats from player_stats table
            PreparedStatement ps = conn.prepareStatement(
                "SELECT name, first_join, last_join, login_count, votes, messages_sent " +
                "FROM player_stats WHERE uuid = ?"
            );
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            
            PlayerStats stats = null;
            
            if (rs.next()) {
                stats = new PlayerStats();
                stats.setUuid(uuid);
                stats.setName(rs.getString("name"));
                stats.setFirstJoin(rs.getLong("first_join"));
                stats.setLastJoin(rs.getLong("last_join"));
                stats.setLoginCount(rs.getInt("login_count"));
                stats.setVotes(rs.getInt("votes"));
                stats.setMessagesSent(rs.getInt("messages_sent"));
            }
            
            rs.close();
            ps.close();
            
            // If stats exist, get online time from online_time table
            if (stats != null) {
                PreparedStatement timePs = conn.prepareStatement(
                    "SELECT total_time FROM online_time WHERE player_uuid = ?"
                );
                timePs.setString(1, uuid.toString());
                ResultSet timeRs = timePs.executeQuery();
                
                if (timeRs.next()) {
                    stats.setTotalOnlineTime(timeRs.getLong("total_time"));
                }
                
                timeRs.close();
                timePs.close();
                
                // Cache the result
                statsCache.put(uuid, stats);
            }
            
            return stats;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching player stats: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get the top players based on a specific statistic
     * 
     * @param statType The type of statistic to sort by
     * @param limit The maximum number of players to return
     * @return A list of PlayerStats objects for the top players
     */
    public List<PlayerStats> getTopPlayers(StatType statType, int limit) {
        List<PlayerStats> topPlayers = new ArrayList<>();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            String query;
            
            // Build query based on stat type
            switch (statType) {
                case ONLINE_TIME:
                    query = "SELECT p.uuid, p.name, ot.total_time, p.first_join, p.login_count, p.votes " +
                            "FROM player_stats p " +
                            "JOIN online_time ot ON p.uuid = ot.player_uuid " +
                            "ORDER BY ot.total_time DESC LIMIT ?";
                    break;
                    
                case LOGIN_COUNT:
                    query = "SELECT uuid, name, first_join, login_count, votes, 0 as total_time " +
                            "FROM player_stats ORDER BY login_count DESC LIMIT ?";
                    break;
                    
                case VOTES:
                    query = "SELECT uuid, name, first_join, login_count, votes, 0 as total_time " +
                            "FROM player_stats ORDER BY votes DESC LIMIT ?";
                    break;
                    
                default:
                    return topPlayers; // Return empty list for invalid stat type
            }
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, limit);
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                PlayerStats stats = new PlayerStats();
                stats.setUuid(UUID.fromString(rs.getString("uuid")));
                stats.setName(rs.getString("name"));
                stats.setFirstJoin(rs.getLong("first_join"));
                stats.setLoginCount(rs.getInt("login_count"));
                stats.setVotes(rs.getInt("votes"));
                stats.setTotalOnlineTime(rs.getLong("total_time"));
                
                topPlayers.add(stats);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching top players: " + e.getMessage());
            e.printStackTrace();
        }
        
        return topPlayers;
    }
    
    /**
     * Invalidate a player's stats in the cache
     * 
     * @param uuid The UUID of the player
     */
    public void invalidateCache(UUID uuid) {
        statsCache.remove(uuid);
    }
    
    /**
     * Clear the entire stats cache
     */
    public void clearCache() {
        statsCache.clear();
    }
    
    /**
     * Format a duration in milliseconds to a human-readable string
     * 
     * @param durationMillis The duration in milliseconds
     * @return A formatted string representing the duration
     */
    public static String formatTimeDuration(long durationMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis) % 24;
        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(days == 1 ? " Tag" : " Tage");
        }
        
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " Stunde" : " Stunden");
        }
        
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " Minute" : " Minuten");
        }
        
        if (seconds > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " Sekunde" : " Sekunden");
        }
        
        return sb.toString();
    }
    
    /**
     * Types of statistics that can be used for top player rankings
     */
    public enum StatType {
        ONLINE_TIME,
        LOGIN_COUNT,
        VOTES
    }
    
    /**
     * Class to represent player statistics
     */
    public static class PlayerStats {
        private UUID uuid;
        private String name;
        private long firstJoin;
        private long lastJoin;
        private int loginCount;
        private int votes;
        private int messagesSent;
        private long totalOnlineTime;
        
        public UUID getUuid() {
            return uuid;
        }
        
        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public long getFirstJoin() {
            return firstJoin;
        }
        
        public void setFirstJoin(long firstJoin) {
            this.firstJoin = firstJoin;
        }
        
        public long getLastJoin() {
            return lastJoin;
        }
        
        public void setLastJoin(long lastJoin) {
            this.lastJoin = lastJoin;
        }
        
        public int getLoginCount() {
            return loginCount;
        }
        
        public void setLoginCount(int loginCount) {
            this.loginCount = loginCount;
        }
        
        public int getVotes() {
            return votes;
        }
        
        public void setVotes(int votes) {
            this.votes = votes;
        }
        
        public int getMessagesSent() {
            return messagesSent;
        }
        
        public void setMessagesSent(int messagesSent) {
            this.messagesSent = messagesSent;
        }
        
        public long getTotalOnlineTime() {
            return totalOnlineTime;
        }
        
        public void setTotalOnlineTime(long totalOnlineTime) {
            this.totalOnlineTime = totalOnlineTime;
        }
    }
} 