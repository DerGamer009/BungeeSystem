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

public class WhoisCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public WhoisCommand() {
        super("whois", "bungeesystem.admin.whois");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent(configManager.getMessage("whois.usage")));
            return;
        }

        String playerName = args[0];
        
        // Check if player is currently online
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(playerName);
        if (target != null && target.isConnected()) {
            // Display online player info
            displayOnlinePlayerInfo(sender, target);
            return;
        }
        
        // Player is offline, check database
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                UUID uuid = getUUIDFromName(playerName);
                if (uuid == null) {
                    sender.sendMessage(new TextComponent(
                            configManager.getMessage("whois.player_not_found", "player", playerName)));
                    return;
                }
                
                // Get player info from database
                displayOfflinePlayerInfo(sender, playerName, uuid);
                
            } catch (SQLException e) {
                sender.sendMessage(new TextComponent(
                        configManager.getMessage("whois.database_error", "error", e.getMessage())));
                plugin.getLogger().severe("Error in WhoisCommand: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Display information about an online player
     * 
     * @param sender The command sender
     * @param player The online player to get info about
     */
    private void displayOnlinePlayerInfo(CommandSender sender, ProxiedPlayer player) {
        UUID uuid = player.getUniqueId();
        String ip = player.getSocketAddress().toString().replace("/", "").split(":")[0];
        String server = player.getServer() != null ? player.getServer().getInfo().getName() : "None";
        boolean isAfk = AfkCommand.isAfk(uuid);
        String nickname = NickCommand.getNickname(uuid);
        
        sender.sendMessage(new TextComponent(configManager.getMessage("whois.header", "player", player.getName())));
        sender.sendMessage(new TextComponent(configManager.getMessage("whois.uuid", "uuid", uuid.toString())));
        sender.sendMessage(new TextComponent(configManager.getMessage("whois.online_status", "status", "Online")));
        sender.sendMessage(new TextComponent(configManager.getMessage("whois.ip", "ip", ip)));
        sender.sendMessage(new TextComponent(configManager.getMessage("whois.current_server", "server", server)));
        
        if (nickname != null) {
            sender.sendMessage(new TextComponent(configManager.getMessage("whois.nickname", "nickname", nickname)));
        }
        
        sender.sendMessage(new TextComponent(configManager.getMessage("whois.afk_status", "status", isAfk ? "Yes" : "No")));
        
        // Get additional info from database (async)
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                long firstJoin = getFirstJoinTime(uuid);
                long totalOnlineTime = getTotalOnlineTime(uuid);
                
                if (firstJoin > 0) {
                    String formattedDate = dateFormat.format(new Date(firstJoin));
                    sender.sendMessage(new TextComponent(configManager.getMessage("whois.first_joined", "date", formattedDate)));
                }
                
                if (totalOnlineTime > 0) {
                    String formattedTime = formatTimeDuration(totalOnlineTime);
                    sender.sendMessage(new TextComponent(configManager.getMessage("whois.total_online_time", "time", formattedTime)));
                }
                
                sender.sendMessage(new TextComponent(configManager.getMessage("whois.footer")));
                
            } catch (SQLException e) {
                sender.sendMessage(new TextComponent(
                        configManager.getMessage("whois.database_error", "error", e.getMessage())));
                plugin.getLogger().severe("Error getting additional player info: " + e.getMessage());
            }
        });
    }
    
    /**
     * Display information about an offline player
     * 
     * @param sender The command sender
     * @param playerName The name of the offline player
     * @param uuid The UUID of the offline player
     * @throws SQLException If a database error occurs
     */
    private void displayOfflinePlayerInfo(CommandSender sender, String playerName, UUID uuid) throws SQLException {
        Connection conn = plugin.getDatabaseManager().getConnection();
        
        PreparedStatement ps = conn.prepareStatement(
                "SELECT ip, first_join, last_seen FROM player_data WHERE uuid = ? LIMIT 1");
        ps.setString(1, uuid.toString());
        
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            String ip = rs.getString("ip");
            long firstJoin = rs.getLong("first_join");
            long lastSeen = rs.getLong("last_seen");
            long totalOnlineTime = getTotalOnlineTime(uuid);
            
            sender.sendMessage(new TextComponent(configManager.getMessage("whois.header", "player", playerName)));
            sender.sendMessage(new TextComponent(configManager.getMessage("whois.uuid", "uuid", uuid.toString())));
            sender.sendMessage(new TextComponent(configManager.getMessage("whois.online_status", "status", "Offline")));
            
            if (ip != null && !ip.isEmpty()) {
                sender.sendMessage(new TextComponent(configManager.getMessage("whois.ip", "ip", ip)));
            }
            
            if (firstJoin > 0) {
                String formattedDate = dateFormat.format(new Date(firstJoin));
                sender.sendMessage(new TextComponent(configManager.getMessage("whois.first_joined", "date", formattedDate)));
            }
            
            if (lastSeen > 0) {
                String formattedDate = dateFormat.format(new Date(lastSeen));
                long timeDiff = System.currentTimeMillis() - lastSeen;
                String timeAgo = formatTimeDifference(timeDiff);
                
                sender.sendMessage(new TextComponent(configManager.getMessage("whois.last_seen", 
                        "date", formattedDate,
                        "time_ago", timeAgo)));
            }
            
            if (totalOnlineTime > 0) {
                String formattedTime = formatTimeDuration(totalOnlineTime);
                sender.sendMessage(new TextComponent(configManager.getMessage("whois.total_online_time", "time", formattedTime)));
            }
            
            sender.sendMessage(new TextComponent(configManager.getMessage("whois.footer")));
        } else {
            sender.sendMessage(new TextComponent(
                    configManager.getMessage("whois.no_additional_info", "player", playerName)));
        }
        
        rs.close();
        ps.close();
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
     * Get a player's first join time
     * 
     * @param uuid The UUID of the player
     * @return The timestamp of the player's first join, or 0 if not found
     * @throws SQLException If a database error occurs
     */
    private long getFirstJoinTime(UUID uuid) throws SQLException {
        Connection conn = plugin.getDatabaseManager().getConnection();
        
        PreparedStatement ps = conn.prepareStatement(
                "SELECT first_join FROM player_data WHERE uuid = ? LIMIT 1");
        ps.setString(1, uuid.toString());
        
        ResultSet rs = ps.executeQuery();
        long firstJoin = 0;
        
        if (rs.next()) {
            firstJoin = rs.getLong("first_join");
        }
        
        rs.close();
        ps.close();
        
        return firstJoin;
    }
    
    /**
     * Get a player's total online time
     * 
     * @param uuid The UUID of the player
     * @return The player's total online time in milliseconds, or 0 if not found
     * @throws SQLException If a database error occurs
     */
    private long getTotalOnlineTime(UUID uuid) throws SQLException {
        Connection conn = plugin.getDatabaseManager().getConnection();
        
        PreparedStatement ps = conn.prepareStatement(
                "SELECT total_time FROM online_time WHERE player_uuid = ? LIMIT 1");
        ps.setString(1, uuid.toString());
        
        ResultSet rs = ps.executeQuery();
        long totalTime = 0;
        
        if (rs.next()) {
            totalTime = rs.getLong("total_time");
        }
        
        rs.close();
        ps.close();
        
        return totalTime;
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
    
    /**
     * Format a duration in milliseconds to a human-readable string
     * 
     * @param durationMillis The duration in milliseconds
     * @return A formatted string representing the duration
     */
    private String formatTimeDuration(long durationMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis) % 24;
        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        
        if (seconds > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }
        
        return sb.toString();
    }
} 