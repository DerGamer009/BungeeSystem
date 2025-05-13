package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SeenCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public SeenCommand() {
        super("seen", "bungeesystem.seen");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent(configManager.getMessage("seen.usage")));
            return;
        }

        String playerName = args[0];
        
        // Check if player is currently online
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(playerName);
        if (target != null && target.isConnected()) {
            sender.sendMessage(new TextComponent(
                    configManager.getMessage("seen.currently_online", "player", target.getName())));
            return;
        }
        
        // Player is offline, check database for last seen time
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                UUID uuid = getUUIDFromName(playerName);
                if (uuid == null) {
                    sender.sendMessage(new TextComponent(
                            configManager.getMessage("seen.player_never_joined", "player", playerName)));
                    return;
                }
                
                // Get last seen time from database
                long lastOnline = getLastOnlineTime(uuid);
                if (lastOnline == 0) {
                    sender.sendMessage(new TextComponent(
                            configManager.getMessage("seen.no_last_seen_data", "player", playerName)));
                    return;
                }
                
                // Format the date
                String formattedDate = dateFormat.format(new Date(lastOnline));
                
                // Calculate time difference
                long timeDiff = System.currentTimeMillis() - lastOnline;
                String timeAgo = formatTimeDifference(timeDiff);
                
                sender.sendMessage(new TextComponent(
                        configManager.getMessage("seen.last_seen",
                                "player", playerName,
                                "date", formattedDate,
                                "time_ago", timeAgo)));
                
            } catch (SQLException e) {
                sender.sendMessage(new TextComponent(
                        configManager.getMessage("seen.database_error", "error", e.getMessage())));
                plugin.getLogger().severe("Error in SeenCommand: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Get a player's UUID from their name using the database
     * 
     * @param playerName The name of the player
     * @return The UUID of the player, or null if not found
     * @throws SQLException If a database error occurs
     */
    private UUID getUUIDFromName(String playerName) throws SQLException {
        Connection conn = plugin.getDatabaseManager().getConnection();
        
        // Check if we have a player_data table, if not create one
        if (!doesTableExist("player_data")) {
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
            return null; // Table just created, no data yet
        }
        
        // Query the player's data
        PreparedStatement ps = conn.prepareStatement(
                "SELECT uuid FROM player_data WHERE name = ? LIMIT 1");
        ps.setString(1, playerName);
        
        ResultSet rs = ps.executeQuery();
        UUID uuid = null;
        
        if (rs.next()) {
            uuid = UUID.fromString(rs.getString("uuid"));
        }
        
        rs.close();
        ps.close();
        
        return uuid;
    }
    
    /**
     * Check if a table exists in the database
     * 
     * @param tableName The name of the table to check
     * @return true if the table exists, false otherwise
     * @throws SQLException If a database error occurs
     */
    private boolean doesTableExist(String tableName) throws SQLException {
        Connection conn = plugin.getDatabaseManager().getConnection();
        ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null);
        boolean exists = rs.next();
        rs.close();
        return exists;
    }
    
    /**
     * Get the last online time for a player
     * 
     * @param uuid The UUID of the player
     * @return The timestamp when the player was last online, or 0 if not found
     * @throws SQLException If a database error occurs
     */
    private long getLastOnlineTime(UUID uuid) throws SQLException {
        Connection conn = plugin.getDatabaseManager().getConnection();
        
        PreparedStatement ps = conn.prepareStatement(
                "SELECT last_seen FROM player_data WHERE uuid = ? LIMIT 1");
        ps.setString(1, uuid.toString());
        
        ResultSet rs = ps.executeQuery();
        long lastSeen = 0;
        
        if (rs.next()) {
            lastSeen = rs.getLong("last_seen");
        }
        
        rs.close();
        ps.close();
        
        return lastSeen;
    }
    
    /**
     * Format a time difference in milliseconds to a human-readable string
     * 
     * @param timeDiffMillis The time difference in milliseconds
     * @return A formatted string representing the time difference
     */
    private String formatTimeDifference(long timeDiffMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiffMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiffMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(timeDiffMillis) % 24;
        long days = TimeUnit.MILLISECONDS.toDays(timeDiffMillis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        
        if (minutes > 0 && days == 0) { // Only show minutes if less than a day
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        
        if (seconds > 0 && hours == 0 && days == 0) { // Only show seconds if less than an hour
            if (sb.length() > 0) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }
        
        if (sb.length() == 0) {
            sb.append("just now");
        } else {
            sb.append(" ago");
        }
        
        return sb.toString();
    }
} 