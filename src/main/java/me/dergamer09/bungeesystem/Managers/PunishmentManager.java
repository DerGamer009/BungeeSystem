package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player punishments (bans, mutes, warnings) and reports
 */
public class PunishmentManager {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    
    // Cache reason IDs to reduce database queries
    private final Map<String, Map<String, Integer>> reasonCache = new HashMap<>();

    public PunishmentManager(BungeeSystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        
        // Initialize reason cache
        initReasonCache();
    }
    
    /**
     * Initialize the cache of punishment reason IDs
     */
    private void initReasonCache() {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                plugin.getLogger().severe("Failed to initialize punishment reason cache: Database connection is null");
                plugin.getLogger().info("Punishment reason cache will be initialized later when database is available");
                return;
            }
            
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, name, type FROM punishment_reasons");
                
                ResultSet rs = ps.executeQuery();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String type = rs.getString("type");
                    
                    // Initialize map for this type if it doesn't exist
                    reasonCache.computeIfAbsent(type, k -> new HashMap<>());
                    
                    // Add the reason to the cache
                    reasonCache.get(type).put(name.toLowerCase(), id);
                }
                
                rs.close();
                ps.close();
                
                plugin.getLogger().info("Punishment reason cache initialized with " + 
                        reasonCache.values().stream().mapToInt(Map::size).sum() + " reasons");
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to initialize punishment reason cache: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Refreshes the reason cache.
     * This can be called after database initialization to ensure cache is populated.
     */
    public void refreshReasonCache() {
        plugin.getLogger().info("Refreshing punishment reason cache...");
        initReasonCache();
    }
    
    /**
     * Get the ID for a punishment reason
     * 
     * @param type The punishment type ("BAN", "MUTE", "WARN", "REPORT")
     * @param reasonName The name of the reason
     * @return The reason ID, or -1 if not found
     */
    public int getReasonId(String type, String reasonName) {
        if (reasonName == null || reasonName.isEmpty()) {
            return -1;
        }
        
        // First check cache
        Map<String, Integer> typeReasons = reasonCache.get(type);
        if (typeReasons != null) {
            Integer id = typeReasons.get(reasonName.toLowerCase());
            if (id != null) {
                return id;
            }
        }
        
        // If not in cache, try to find in database
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM punishment_reasons WHERE type = ? AND LOWER(name) = LOWER(?)");
            ps.setString(1, type);
            ps.setString(2, reasonName);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("id");
                
                // Add to cache
                if (typeReasons == null) {
                    typeReasons = new HashMap<>();
                    reasonCache.put(type, typeReasons);
                }
                typeReasons.put(reasonName.toLowerCase(), id);
                
                rs.close();
                ps.close();
                return id;
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get reason ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * Get all available punishment reasons for a specific type
     * 
     * @param type The punishment type ("BAN", "MUTE", "WARN", "REPORT")
     * @return List of reason objects with id, name, description, and duration
     */
    public List<Map<String, Object>> getReasons(String type) {
        List<Map<String, Object>> reasons = new ArrayList<>();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, description, duration FROM punishment_reasons WHERE type = ? ORDER BY name");
            ps.setString(1, type);
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> reason = new HashMap<>();
                reason.put("id", rs.getInt("id"));
                reason.put("name", rs.getString("name"));
                reason.put("description", rs.getString("description"));
                reason.put("duration", rs.getLong("duration"));
                
                reasons.add(reason);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get punishment reasons: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reasons;
    }
    
    /**
     * Ban a player
     * 
     * @param sender Who issued the ban
     * @param targetName Name of the player to ban
     * @param reasonId ID of the ban reason (from punishment_reasons table)
     * @param customReason Optional custom reason text
     * @param duration Duration in milliseconds, -1 for permanent
     * @return true if successful, false if player not found or already banned
     */
    public boolean banPlayer(CommandSender sender, String targetName, int reasonId, String customReason, long duration) {
        // Check if the player is online
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
        UUID targetUUID;
        String ip = null;
        
        if (target != null) {
            // Player is online
            targetUUID = target.getUniqueId();
            ip = target.getSocketAddress().toString().replace("/", "").split(":")[0];
        } else {
            // Player is offline, try to get UUID from database
            targetUUID = plugin.getDatabaseManager().getUUIDFromName(targetName);
            
            if (targetUUID == null) {
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.player_not_found", 
                        "player", targetName)));
                return false;
            }
        }
        
        // Check if the player is already banned
        if (isPlayerBanned(targetUUID)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.already_banned", 
                    "player", targetName)));
            return false;
        }
        
        // Ban the player
        return banPlayer(sender, targetUUID, targetName, reasonId, customReason, duration, ip);
    }
    
    /**
     * Ban a player by UUID
     * 
     * @param sender Who issued the ban
     * @param targetUUID UUID of the player to ban
     * @param targetName Name of the player to ban
     * @param reasonId ID of the ban reason
     * @param customReason Optional custom reason text
     * @param duration Duration in milliseconds, -1 for permanent
     * @param ip Player's IP address (can be null)
     * @return true if successful
     */
    public boolean banPlayer(CommandSender sender, UUID targetUUID, String targetName, int reasonId, 
            String customReason, long duration, String ip) {
        
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String senderUUID = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "CONSOLE";
        
        long now = System.currentTimeMillis();
        long expireTime = duration < 0 ? -1 : now + duration;
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bans (player_uuid, player_name, banned_by, banned_by_name, reason_id, reason, " +
                    "timestamp, expire_timestamp, ip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, targetUUID.toString());
            ps.setString(2, targetName);
            ps.setString(3, senderUUID);
            ps.setString(4, senderName);
            ps.setInt(5, reasonId);
            ps.setString(6, customReason);
            ps.setLong(7, now);
            ps.setLong(8, expireTime);
            ps.setString(9, ip);
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            int banId = rs.next() ? rs.getInt(1) : -1;
            
            rs.close();
            ps.close();
            
            // If the player is online, kick them
            ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetUUID);
            if (target != null) {
                kickBannedPlayer(target, reasonId, customReason, expireTime);
            }
            
            // Get the reason name
            String reasonName = getReasonName(reasonId);
            
            // Build duration string
            String durationStr = duration < 0 ? 
                    configManager.getMessage("punishment.permanent") : 
                    formatDuration(duration);
            
            // Notify the sender
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.ban_success",
                    "player", targetName,
                    "reason", reasonName,
                    "duration", durationStr,
                    "id", String.valueOf(banId))));
            
            // Broadcast to staff if enabled
            if (plugin.getConfig().getBoolean("punishments.broadcast_to_staff", true)) {
                broadcastToStaff(configManager.getMessage("punishment.ban_broadcast",
                        "player", targetName,
                        "reason", reasonName,
                        "duration", durationStr,
                        "staff", senderName,
                        "id", String.valueOf(banId)));
            }
            
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to ban player: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.error",
                    "error", e.getMessage())));
            return false;
        }
    }
    
    /**
     * Kick a banned player with the appropriate message
     * 
     * @param player The player to kick
     * @param reasonId The reason ID
     * @param customReason Custom reason text (can be null)
     * @param expireTime When the ban expires (-1 for permanent)
     */
    private void kickBannedPlayer(ProxiedPlayer player, int reasonId, String customReason, long expireTime) {
        String reasonName = getReasonName(reasonId);
        String reason = customReason != null && !customReason.isEmpty() ? 
                reasonName + ": " + customReason : reasonName;
        
        String expireStr = expireTime < 0 ? 
                configManager.getMessage("punishment.permanent") : 
                dateFormat.format(new Date(expireTime));
        
        String kickMessage = configManager.getMessage("punishment.ban_kick_message",
                "reason", reason,
                "expire", expireStr);
        
        player.disconnect(new TextComponent(kickMessage));
    }
    
    /**
     * Check if a player is currently banned
     * 
     * @param uuid The UUID of the player to check
     * @return true if the player is banned
     */
    public boolean isPlayerBanned(UUID uuid) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Check for active bans
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM bans WHERE player_uuid = ? AND active = TRUE " + 
                    "AND (expire_timestamp > ? OR expire_timestamp = -1) LIMIT 1");
            
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            
            ResultSet rs = ps.executeQuery();
            boolean isBanned = rs.next();
            
            rs.close();
            ps.close();
            
            return isBanned;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check if player is banned: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get ban information for a player
     * 
     * @param uuid The UUID of the player
     * @return Map containing ban information, or null if not banned
     */
    public Map<String, Object> getPlayerBan(UUID uuid) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT b.*, pr.name as reason_name, pr.description as reason_description " +
                    "FROM bans b " +
                    "LEFT JOIN punishment_reasons pr ON b.reason_id = pr.id " +
                    "WHERE b.player_uuid = ? AND b.active = TRUE " + 
                    "AND (b.expire_timestamp > ? OR b.expire_timestamp = -1) " +
                    "ORDER BY b.id DESC LIMIT 1");
            
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> banInfo = new HashMap<>();
                banInfo.put("id", rs.getInt("id"));
                banInfo.put("reason_id", rs.getInt("reason_id"));
                banInfo.put("reason_name", rs.getString("reason_name"));
                banInfo.put("reason_description", rs.getString("reason_description"));
                banInfo.put("custom_reason", rs.getString("reason"));
                banInfo.put("banned_by", rs.getString("banned_by_name"));
                banInfo.put("timestamp", rs.getLong("timestamp"));
                banInfo.put("expire_timestamp", rs.getLong("expire_timestamp"));
                
                rs.close();
                ps.close();
                
                return banInfo;
            }
            
            rs.close();
            ps.close();
            
            return null;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player ban: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Unban a player
     * 
     * @param sender Who is unbanning the player
     * @param targetName Name of the player to unban
     * @param reason Reason for the unban
     * @return true if successful
     */
    public boolean unbanPlayer(CommandSender sender, String targetName, String reason) {
        // Try to get UUID from player name
        UUID targetUUID = plugin.getDatabaseManager().getUUIDFromName(targetName);
        
        if (targetUUID == null) {
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.player_not_found", 
                    "player", targetName)));
            return false;
        }
        
        // Check if the player is actually banned
        if (!isPlayerBanned(targetUUID)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.not_banned", 
                    "player", targetName)));
            return false;
        }
        
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String senderUUID = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "CONSOLE";
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Update all active bans for this player to inactive
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE bans SET active = FALSE, unbanned_by = ?, unbanned_by_name = ?, " +
                    "unban_timestamp = ?, unban_reason = ? " +
                    "WHERE player_uuid = ? AND active = TRUE");
            
            ps.setString(1, senderUUID);
            ps.setString(2, senderName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, reason);
            ps.setString(5, targetUUID.toString());
            
            int updated = ps.executeUpdate();
            ps.close();
            
            if (updated > 0) {
                // Notify the sender
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.unban_success",
                        "player", targetName)));
                
                // Broadcast to staff if enabled
                if (plugin.getConfig().getBoolean("punishments.broadcast_to_staff", true)) {
                    broadcastToStaff(configManager.getMessage("punishment.unban_broadcast",
                            "player", targetName,
                            "reason", reason,
                            "staff", senderName));
                }
                
                return true;
            } else {
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.not_banned", 
                        "player", targetName)));
                return false;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unban player: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.error",
                    "error", e.getMessage())));
            return false;
        }
    }
    
    /**
     * Get the name of a punishment reason by ID
     * 
     * @param reasonId The reason ID
     * @return The reason name, or "Unknown reason" if not found
     */
    private String getReasonName(int reasonId) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT name FROM punishment_reasons WHERE id = ?");
            ps.setInt(1, reasonId);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String name = rs.getString("name");
                rs.close();
                ps.close();
                return name;
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get reason name: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "Unknown reason";
    }
    
    /**
     * Format a duration in milliseconds to a human-readable string
     * 
     * @param duration Duration in milliseconds
     * @return Formatted duration string
     */
    private String formatDuration(long duration) {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + (days == 1 ? " Tag" : " Tage");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " Stunde" : " Stunden");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " Minute" : " Minuten");
        } else {
            return seconds + (seconds == 1 ? " Sekunde" : " Sekunden");
        }
    }
    
    /**
     * Broadcast a message to all staff members
     * 
     * @param message The message to broadcast
     */
    private void broadcastToStaff(String message) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.hasPermission("bungeesystem.staff")) {
                player.sendMessage(new TextComponent(message));
            }
        }
        
        // Also send to console
        ProxyServer.getInstance().getConsole().sendMessage(new TextComponent(message));
    }
    
    /**
     * Mute a player
     * 
     * @param sender Who issued the mute
     * @param targetName Name of the player to mute
     * @param reasonId ID of the mute reason (from punishment_reasons table)
     * @param customReason Optional custom reason text
     * @param duration Duration in milliseconds, -1 for permanent
     * @return true if successful, false if player not found or already muted
     */
    public boolean mutePlayer(CommandSender sender, String targetName, int reasonId, String customReason, long duration) {
        // Get the player UUID 
        UUID targetUUID;
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
        
        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            targetUUID = plugin.getDatabaseManager().getUUIDFromName(targetName);
            
            if (targetUUID == null) {
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.player_not_found", 
                        "player", targetName)));
                return false;
            }
        }
        
        // Check if the player is already muted
        if (isPlayerMuted(targetUUID)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.already_muted", 
                    "player", targetName)));
            return false;
        }
        
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String senderUUID = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "CONSOLE";
        
        long now = System.currentTimeMillis();
        long expireTime = duration < 0 ? -1 : now + duration;
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO mutes (player_uuid, player_name, muted_by, muted_by_name, reason_id, reason, " +
                    "timestamp, expire_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, targetUUID.toString());
            ps.setString(2, targetName);
            ps.setString(3, senderUUID);
            ps.setString(4, senderName);
            ps.setInt(5, reasonId);
            ps.setString(6, customReason);
            ps.setLong(7, now);
            ps.setLong(8, expireTime);
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            int muteId = rs.next() ? rs.getInt(1) : -1;
            
            rs.close();
            ps.close();
            
            // Get the reason name
            String reasonName = getReasonName(reasonId);
            
            // Build duration string
            String durationStr = duration < 0 ? 
                    configManager.getMessage("punishment.permanent") : 
                    formatDuration(duration);
            
            // Notify the sender
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.mute_success",
                    "player", targetName,
                    "reason", reasonName,
                    "duration", durationStr,
                    "id", String.valueOf(muteId))));
            
            // Notify the target if they are online
            if (target != null) {
                target.sendMessage(new TextComponent(configManager.getMessage("punishment.mute_notification",
                        "reason", reasonName + (customReason != null && !customReason.isEmpty() ? ": " + customReason : ""),
                        "duration", durationStr,
                        "staff", senderName)));
            }
            
            // Broadcast to staff if enabled
            if (plugin.getConfig().getBoolean("punishments.broadcast_to_staff", true)) {
                broadcastToStaff(configManager.getMessage("punishment.mute_broadcast",
                        "player", targetName,
                        "reason", reasonName,
                        "duration", durationStr,
                        "staff", senderName,
                        "id", String.valueOf(muteId)));
            }
            
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to mute player: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.error",
                    "error", e.getMessage())));
            return false;
        }
    }
    
    /**
     * Check if a player is currently muted
     * 
     * @param uuid The UUID of the player to check
     * @return true if the player is muted
     */
    public boolean isPlayerMuted(UUID uuid) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Check for active mutes
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM mutes WHERE player_uuid = ? AND active = TRUE " + 
                    "AND (expire_timestamp > ? OR expire_timestamp = -1) LIMIT 1");
            
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            
            ResultSet rs = ps.executeQuery();
            boolean isMuted = rs.next();
            
            rs.close();
            ps.close();
            
            return isMuted;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check if player is muted: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get mute information for a player
     * 
     * @param uuid The UUID of the player
     * @return Map containing mute information, or null if not muted
     */
    public Map<String, Object> getPlayerMute(UUID uuid) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT m.*, pr.name as reason_name, pr.description as reason_description " +
                    "FROM mutes m " +
                    "LEFT JOIN punishment_reasons pr ON m.reason_id = pr.id " +
                    "WHERE m.player_uuid = ? AND m.active = TRUE " + 
                    "AND (m.expire_timestamp > ? OR m.expire_timestamp = -1) " +
                    "ORDER BY m.id DESC LIMIT 1");
            
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> muteInfo = new HashMap<>();
                muteInfo.put("id", rs.getInt("id"));
                muteInfo.put("reason_id", rs.getInt("reason_id"));
                muteInfo.put("reason_name", rs.getString("reason_name"));
                muteInfo.put("reason_description", rs.getString("reason_description"));
                muteInfo.put("custom_reason", rs.getString("reason"));
                muteInfo.put("muted_by", rs.getString("muted_by_name"));
                muteInfo.put("timestamp", rs.getLong("timestamp"));
                muteInfo.put("expire_timestamp", rs.getLong("expire_timestamp"));
                
                rs.close();
                ps.close();
                
                return muteInfo;
            }
            
            rs.close();
            ps.close();
            
            return null;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player mute: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Unmute a player
     * 
     * @param sender Who is unmuting the player
     * @param targetName Name of the player to unmute
     * @param reason Reason for the unmute
     * @return true if successful
     */
    public boolean unmutePlayer(CommandSender sender, String targetName, String reason) {
        // Try to get UUID from player name
        UUID targetUUID = plugin.getDatabaseManager().getUUIDFromName(targetName);
        
        if (targetUUID == null) {
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.player_not_found", 
                    "player", targetName)));
            return false;
        }
        
        // Check if the player is actually muted
        if (!isPlayerMuted(targetUUID)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.not_muted", 
                    "player", targetName)));
            return false;
        }
        
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String senderUUID = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "CONSOLE";
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Update all active mutes for this player to inactive
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE mutes SET active = FALSE, unmuted_by = ?, unmuted_by_name = ?, " +
                    "unmute_timestamp = ?, unmute_reason = ? " +
                    "WHERE player_uuid = ? AND active = TRUE");
            
            ps.setString(1, senderUUID);
            ps.setString(2, senderName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, reason);
            ps.setString(5, targetUUID.toString());
            
            int updated = ps.executeUpdate();
            ps.close();
            
            if (updated > 0) {
                // Notify the sender
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.unmute_success",
                        "player", targetName)));
                
                // Notify the player if they're online
                ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetUUID);
                if (target != null) {
                    target.sendMessage(new TextComponent(configManager.getMessage("punishment.unmute_notification",
                            "staff", senderName)));
                }
                
                // Broadcast to staff if enabled
                if (plugin.getConfig().getBoolean("punishments.broadcast_to_staff", true)) {
                    broadcastToStaff(configManager.getMessage("punishment.unmute_broadcast",
                            "player", targetName,
                            "reason", reason,
                            "staff", senderName));
                }
                
                return true;
            } else {
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.not_muted", 
                        "player", targetName)));
                return false;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unmute player: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.error",
                    "error", e.getMessage())));
            return false;
        }
    }
    
    /**
     * Warn a player
     * 
     * @param sender Who issued the warning
     * @param targetName Name of the player to warn
     * @param reasonId ID of the warning reason (from punishment_reasons table)
     * @param customReason Optional custom reason text
     * @return true if successful, false if player not found
     */
    public boolean warnPlayer(CommandSender sender, String targetName, int reasonId, String customReason) {
        // Get the player UUID
        UUID targetUUID;
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
        
        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            targetUUID = plugin.getDatabaseManager().getUUIDFromName(targetName);
            
            if (targetUUID == null) {
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.player_not_found", 
                        "player", targetName)));
                return false;
            }
        }
        
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String senderUUID = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "CONSOLE";
        
        long now = System.currentTimeMillis();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO warnings (player_uuid, player_name, warned_by, warned_by_name, reason_id, reason, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, targetUUID.toString());
            ps.setString(2, targetName);
            ps.setString(3, senderUUID);
            ps.setString(4, senderName);
            ps.setInt(5, reasonId);
            ps.setString(6, customReason);
            ps.setLong(7, now);
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            int warnId = rs.next() ? rs.getInt(1) : -1;
            
            rs.close();
            ps.close();
            
            // Get the reason name
            String reasonName = getReasonName(reasonId);
            
            // Notify the sender
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.warn_success",
                    "player", targetName,
                    "reason", reasonName,
                    "id", String.valueOf(warnId))));
            
            // Notify the target if they are online
            if (target != null) {
                target.sendMessage(new TextComponent(configManager.getMessage("punishment.warn_notification",
                        "reason", reasonName + (customReason != null && !customReason.isEmpty() ? ": " + customReason : ""),
                        "staff", senderName)));
            }
            
            // Check warning count for auto-punishments if enabled
            if (plugin.getConfig().getBoolean("punishments.auto_punish_warnings", false)) {
                checkWarningCount(targetUUID, targetName);
            }
            
            // Broadcast to staff if enabled
            if (plugin.getConfig().getBoolean("punishments.broadcast_to_staff", true)) {
                broadcastToStaff(configManager.getMessage("punishment.warn_broadcast",
                        "player", targetName,
                        "reason", reasonName,
                        "staff", senderName,
                        "id", String.valueOf(warnId)));
            }
            
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to warn player: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.error",
                    "error", e.getMessage())));
            return false;
        }
    }
    
    /**
     * Check a player's warning count for auto-punishments
     * 
     * @param uuid The player's UUID
     * @param playerName The player's name
     */
    private void checkWarningCount(UUID uuid, String playerName) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Count active warnings
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM warnings WHERE player_uuid = ? AND active = TRUE");
            
            ps.setString(1, uuid.toString());
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int warningCount = rs.getInt(1);
                rs.close();
                ps.close();
                
                // Check for auto-punishments based on warning count
                int warnsToBan = plugin.getConfig().getInt("punishments.warnings_to_ban", -1);
                int warnsToMute = plugin.getConfig().getInt("punishments.warnings_to_mute", -1);
                
                // Auto-ban if configured and reached threshold
                if (warnsToBan > 0 && warningCount >= warnsToBan) {
                    // Get configured auto-ban reason and duration
                    int banReasonId = plugin.getConfig().getInt("punishments.auto_ban_reason_id", -1);
                    long banDuration = plugin.getConfig().getLong("punishments.auto_ban_duration", -1);
                    
                    if (banReasonId > 0) {
                        banPlayer(ProxyServer.getInstance().getConsole(), uuid, playerName, banReasonId, 
                                "Auto-ban after " + warningCount + " warnings", banDuration, null);
                    }
                }
                // Auto-mute if configured and reached threshold (but not banned)
                else if (warnsToMute > 0 && warningCount >= warnsToMute) {
                    // Get configured auto-mute reason and duration
                    int muteReasonId = plugin.getConfig().getInt("punishments.auto_mute_reason_id", -1);
                    long muteDuration = plugin.getConfig().getLong("punishments.auto_mute_duration", -1);
                    
                    if (muteReasonId > 0 && !isPlayerMuted(uuid)) {
                        mutePlayer(ProxyServer.getInstance().getConsole(), playerName, muteReasonId, 
                                "Auto-mute after " + warningCount + " warnings", muteDuration);
                    }
                }
            } else {
                rs.close();
                ps.close();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check warning count: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get a player's active warnings
     * 
     * @param uuid The UUID of the player
     * @return List of warning info maps
     */
    public List<Map<String, Object>> getPlayerWarnings(UUID uuid) {
        List<Map<String, Object>> warnings = new ArrayList<>();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT w.*, pr.name as reason_name, pr.description as reason_description " +
                    "FROM warnings w " +
                    "LEFT JOIN punishment_reasons pr ON w.reason_id = pr.id " +
                    "WHERE w.player_uuid = ? AND w.active = TRUE " +
                    "ORDER BY w.id DESC");
            
            ps.setString(1, uuid.toString());
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> warning = new HashMap<>();
                warning.put("id", rs.getInt("id"));
                warning.put("reason_id", rs.getInt("reason_id"));
                warning.put("reason_name", rs.getString("reason_name"));
                warning.put("reason_description", rs.getString("reason_description"));
                warning.put("custom_reason", rs.getString("reason"));
                warning.put("warned_by", rs.getString("warned_by_name"));
                warning.put("timestamp", rs.getLong("timestamp"));
                
                warnings.add(warning);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player warnings: " + e.getMessage());
            e.printStackTrace();
        }
        
        return warnings;
    }
    
    /**
     * Delete a warning
     * 
     * @param sender Who is deleting the warning
     * @param warningId ID of the warning to delete
     * @param reason Reason for deletion
     * @return true if successful
     */
    public boolean deleteWarning(CommandSender sender, int warningId, String reason) {
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String senderUUID = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "CONSOLE";
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // First get the warning details
            PreparedStatement psGet = conn.prepareStatement(
                    "SELECT player_uuid, player_name FROM warnings WHERE id = ? AND active = TRUE");
            
            psGet.setInt(1, warningId);
            
            ResultSet rs = psGet.executeQuery();
            
            if (!rs.next()) {
                rs.close();
                psGet.close();
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.warning_not_found", 
                        "id", String.valueOf(warningId))));
                return false;
            }
            
            String playerUUID = rs.getString("player_uuid");
            String playerName = rs.getString("player_name");
            
            rs.close();
            psGet.close();
            
            // Now delete (deactivate) the warning
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE warnings SET active = FALSE, removed_by = ?, removed_by_name = ?, " +
                    "removal_timestamp = ?, removal_reason = ? " +
                    "WHERE id = ? AND active = TRUE");
            
            ps.setString(1, senderUUID);
            ps.setString(2, senderName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, reason);
            ps.setInt(5, warningId);
            
            int updated = ps.executeUpdate();
            ps.close();
            
            if (updated > 0) {
                // Notify the sender
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.warning_deleted",
                        "id", String.valueOf(warningId),
                        "player", playerName)));
                
                // Broadcast to staff if enabled
                if (plugin.getConfig().getBoolean("punishments.broadcast_to_staff", true)) {
                    broadcastToStaff(configManager.getMessage("punishment.warning_delete_broadcast",
                            "player", playerName,
                            "id", String.valueOf(warningId),
                            "reason", reason,
                            "staff", senderName));
                }
                
                return true;
            } else {
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.warning_not_found", 
                        "id", String.valueOf(warningId))));
                return false;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete warning: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.error",
                    "error", e.getMessage())));
            return false;
        }
    }
    
    /**
     * Report a player
     * 
     * @param sender Who issued the report
     * @param targetName Name of the player to report
     * @param reasonId ID of the report reason (from punishment_reasons table)
     * @param customReason Optional custom reason text
     * @return true if successful, false if player not found
     */
    public boolean reportPlayer(CommandSender sender, String targetName, int reasonId, String customReason) {
        // Get the player UUID
        UUID targetUUID = plugin.getDatabaseManager().getUUIDFromName(targetName);
        
        if (targetUUID == null) {
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.player_not_found", 
                    "player", targetName)));
            return false;
        }
        
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String senderUUID = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "CONSOLE";
        
        // Check if the player is reporting themselves
        if (sender instanceof ProxiedPlayer && targetUUID.equals(((ProxiedPlayer) sender).getUniqueId())) {
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.cannot_report_self")));
            return false;
        }
        
        // Check if the player has already reported this player recently if cooldown is enabled
        int cooldownSeconds = plugin.getConfig().getInt("punishments.report_cooldown_seconds", 0);
        if (cooldownSeconds > 0 && sender instanceof ProxiedPlayer) {
            if (hasReportCooldown(((ProxiedPlayer) sender).getUniqueId(), targetUUID, cooldownSeconds)) {
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.report_cooldown",
                        "player", targetName,
                        "time", String.valueOf(cooldownSeconds))));
                return false;
            }
        }
        
        long now = System.currentTimeMillis();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO reports (player_uuid, player_name, reporter_uuid, reporter_name, reason_id, reason, " + 
                    "timestamp, server) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, targetUUID.toString());
            ps.setString(2, targetName);
            ps.setString(3, senderUUID);
            ps.setString(4, senderName);
            ps.setInt(5, reasonId);
            ps.setString(6, customReason);
            ps.setLong(7, now);
            
            // Get server name if player is online
            String serverName = "Unknown";
            ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.getServer() != null) {
                serverName = targetPlayer.getServer().getInfo().getName();
            }
            ps.setString(8, serverName);
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            int reportId = rs.next() ? rs.getInt(1) : -1;
            
            rs.close();
            ps.close();
            
            // Get the reason name
            String reasonName = getReasonName(reasonId);
            
            // Notify the sender
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.report_success",
                    "player", targetName,
                    "reason", reasonName,
                    "id", String.valueOf(reportId))));
            
            // Notify online staff members
            notifyStaffOfReport(targetName, senderName, reasonName, customReason, serverName, reportId);
            
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to report player: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.error",
                    "error", e.getMessage())));
            return false;
        }
    }
    
    /**
     * Check if a player has a reporting cooldown for a specific target
     * 
     * @param reporterUUID UUID of the reporter
     * @param targetUUID UUID of the target
     * @param cooldownSeconds Cooldown time in seconds
     * @return true if the player reported this target recently
     */
    private boolean hasReportCooldown(UUID reporterUUID, UUID targetUUID, int cooldownSeconds) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            long cooldownTime = System.currentTimeMillis() - (cooldownSeconds * 1000L);
            
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM reports WHERE reporter_uuid = ? AND player_uuid = ? AND timestamp > ?");
            
            ps.setString(1, reporterUUID.toString());
            ps.setString(2, targetUUID.toString());
            ps.setLong(3, cooldownTime);
            
            ResultSet rs = ps.executeQuery();
            boolean hasCooldown = rs.next();
            
            rs.close();
            ps.close();
            
            return hasCooldown;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check report cooldown: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Notify online staff members about a new report
     * 
     * @param playerName Reported player name
     * @param reporterName Reporter name
     * @param reasonName Report reason name
     * @param customReason Custom reason text (can be null)
     * @param serverName Server where the reported player is
     * @param reportId Report ID
     */
    private void notifyStaffOfReport(String playerName, String reporterName, String reasonName, 
            String customReason, String serverName, int reportId) {
        
        String formattedReason = reasonName;
        if (customReason != null && !customReason.isEmpty()) {
            formattedReason += ": " + customReason;
        }
        
        String notification = configManager.getMessage("punishment.report_notification",
                "player", playerName,
                "reporter", reporterName,
                "reason", formattedReason,
                "server", serverName,
                "id", String.valueOf(reportId));
        
        // Send to all staff online
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.hasPermission("bungeesystem.reports.receive")) {
                player.sendMessage(new TextComponent(notification));
            }
        }
        
        // Also log to console
        ProxyServer.getInstance().getConsole().sendMessage(new TextComponent(notification));

        // Send notification to Discord webhook if configured
        String webhookUrl = plugin.getWebhookUrl();
        if (webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.equals("https://your-discord-webhook-url.com")) {
            String format = plugin.getConfig().getString("reportWebhookFormat",
                    "New report #%id% against %player% by %reporter% on %server%: %reason%");
            String webhookMessage = format
                    .replace("%id%", String.valueOf(reportId))
                    .replace("%player%", playerName)
                    .replace("%reporter%", reporterName)
                    .replace("%server%", serverName)
                    .replace("%reason%", formattedReason);
            me.dergamer09.bungeesystem.util.WebhookUtil.sendWebhook(webhookUrl, webhookMessage, plugin);
        }
    }
    
    /**
     * Get a list of active reports
     * 
     * @param limit Maximum number of reports to return (0 for all)
     * @return List of report info maps
     */
    public List<Map<String, Object>> getActiveReports(int limit) {
        List<Map<String, Object>> reports = new ArrayList<>();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            String query = "SELECT r.*, pr.name as reason_name, pr.description as reason_description " +
                    "FROM reports r " +
                    "LEFT JOIN punishment_reasons pr ON r.reason_id = pr.id " +
                    "WHERE r.status = 'OPEN' " +
                    "ORDER BY r.id DESC";
            
            if (limit > 0) {
                query += " LIMIT " + limit;
            }
            
            PreparedStatement ps = conn.prepareStatement(query);
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> report = new HashMap<>();
                report.put("id", rs.getInt("id"));
                report.put("player_uuid", rs.getString("player_uuid"));
                report.put("player_name", rs.getString("player_name"));
                report.put("reporter_uuid", rs.getString("reporter_uuid"));
                report.put("reporter_name", rs.getString("reporter_name"));
                report.put("reason_id", rs.getInt("reason_id"));
                report.put("reason_name", rs.getString("reason_name"));
                report.put("reason_description", rs.getString("reason_description"));
                report.put("custom_reason", rs.getString("reason"));
                report.put("timestamp", rs.getLong("timestamp"));
                report.put("server", rs.getString("server"));
                
                reports.add(report);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get active reports: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reports;
    }
    
    /**
     * Handle a report (mark as CLOSED)
     * 
     * @param sender Who is handling the report
     * @param reportId ID of the report to handle
     * @param comment Comment about how the report was handled
     * @return true if successful
     */
    public boolean handleReport(CommandSender sender, int reportId, String comment) {
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String senderUUID = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "CONSOLE";
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // First get the report details
            PreparedStatement psGet = conn.prepareStatement(
                    "SELECT player_name, reporter_name FROM reports WHERE id = ? AND status = 'OPEN'");
            
            psGet.setInt(1, reportId);
            
            ResultSet rs = psGet.executeQuery();
            
            if (!rs.next()) {
                rs.close();
                psGet.close();
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.report_not_found", 
                        "id", String.valueOf(reportId))));
                return false;
            }
            
            String playerName = rs.getString("player_name");
            String reporterName = rs.getString("reporter_name");
            
            rs.close();
            psGet.close();
            
            // Now update the report
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reports SET status = 'CLOSED', handled_by = ?, handled_by_name = ?, " +
                    "handled_timestamp = ?, handling_comment = ? " +
                    "WHERE id = ? AND status = 'OPEN'");
            
            ps.setString(1, senderUUID);
            ps.setString(2, senderName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, comment);
            ps.setInt(5, reportId);
            
            int updated = ps.executeUpdate();
            ps.close();
            
            if (updated > 0) {
                // Notify the sender
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.report_handled",
                        "id", String.valueOf(reportId),
                        "player", playerName)));
                
                // Notify the reporter if they're online and notification is enabled
                if (plugin.getConfig().getBoolean("punishments.notify_reporter_on_handled", true)) {
                    ProxiedPlayer reporter = ProxyServer.getInstance().getPlayer(reporterName);
                    if (reporter != null) {
                        reporter.sendMessage(new TextComponent(configManager.getMessage("punishment.report_handled_notification",
                                "player", playerName,
                                "staff", senderName)));
                    }
                }
                
                // Broadcast to staff if enabled
                if (plugin.getConfig().getBoolean("punishments.broadcast_to_staff", true)) {
                    broadcastToStaff(configManager.getMessage("punishment.report_handled_broadcast",
                            "player", playerName,
                            "id", String.valueOf(reportId),
                            "staff", senderName));
                }
                
                return true;
            } else {
                sender.sendMessage(new TextComponent(configManager.getMessage("punishment.report_not_found", 
                        "id", String.valueOf(reportId))));
                return false;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to handle report: " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(new TextComponent(configManager.getMessage("punishment.error",
                    "error", e.getMessage())));
            return false;
        }
    }
    
    /**
     * Get a player's punishment history
     * 
     * @param uuid The UUID of the player
     * @param includeActive Whether to include active punishments
     * @param limit Maximum number of items to return (0 for all)
     * @return List of punishment info maps
     */
    public List<Map<String, Object>> getPlayerPunishmentHistory(UUID uuid, boolean includeActive, int limit) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Build the query for bans
            StringBuilder banQuery = new StringBuilder();
            banQuery.append("SELECT 'BAN' as type, b.id, b.player_uuid, b.player_name, b.banned_by_name as punisher_name, ");
            banQuery.append("b.reason_id, pr.name as reason_name, b.reason as custom_reason, ");
            banQuery.append("b.timestamp, b.expire_timestamp, b.active, ");
            banQuery.append("b.unbanned_by_name as removed_by_name, b.unban_timestamp as removal_timestamp, ");
            banQuery.append("b.unban_reason as removal_reason ");
            banQuery.append("FROM bans b ");
            banQuery.append("LEFT JOIN punishment_reasons pr ON b.reason_id = pr.id ");
            banQuery.append("WHERE b.player_uuid = ? ");
            
            if (!includeActive) {
                banQuery.append("AND b.active = FALSE ");
            }
            
            // Build the query for mutes
            StringBuilder muteQuery = new StringBuilder();
            muteQuery.append("SELECT 'MUTE' as type, m.id, m.player_uuid, m.player_name, m.muted_by_name as punisher_name, ");
            muteQuery.append("m.reason_id, pr.name as reason_name, m.reason as custom_reason, ");
            muteQuery.append("m.timestamp, m.expire_timestamp, m.active, ");
            muteQuery.append("m.unmuted_by_name as removed_by_name, m.unmute_timestamp as removal_timestamp, ");
            muteQuery.append("m.unmute_reason as removal_reason ");
            muteQuery.append("FROM mutes m ");
            muteQuery.append("LEFT JOIN punishment_reasons pr ON m.reason_id = pr.id ");
            muteQuery.append("WHERE m.player_uuid = ? ");
            
            if (!includeActive) {
                muteQuery.append("AND m.active = FALSE ");
            }
            
            // Build the query for warnings
            StringBuilder warnQuery = new StringBuilder();
            warnQuery.append("SELECT 'WARN' as type, w.id, w.player_uuid, w.player_name, w.warned_by_name as punisher_name, ");
            warnQuery.append("w.reason_id, pr.name as reason_name, w.reason as custom_reason, ");
            warnQuery.append("w.timestamp, -1 as expire_timestamp, w.active, ");
            warnQuery.append("w.removed_by_name, w.removal_timestamp, ");
            warnQuery.append("w.removal_reason ");
            warnQuery.append("FROM warnings w ");
            warnQuery.append("LEFT JOIN punishment_reasons pr ON w.reason_id = pr.id ");
            warnQuery.append("WHERE w.player_uuid = ? ");
            
            if (!includeActive) {
                warnQuery.append("AND w.active = FALSE ");
            }
            
            // Combine the queries with UNION
            String query = banQuery.toString() + 
                    " UNION " + muteQuery.toString() + 
                    " UNION " + warnQuery.toString() + 
                    " ORDER BY timestamp DESC";
            
            if (limit > 0) {
                query += " LIMIT " + limit;
            }
            
            PreparedStatement ps = conn.prepareStatement(query);
            
            // Set parameters for each subquery
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            ps.setString(3, uuid.toString());
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> punishment = new HashMap<>();
                punishment.put("type", rs.getString("type"));
                punishment.put("id", rs.getInt("id"));
                punishment.put("reason_id", rs.getInt("reason_id"));
                punishment.put("reason_name", rs.getString("reason_name"));
                punishment.put("custom_reason", rs.getString("custom_reason"));
                punishment.put("punisher_name", rs.getString("punisher_name"));
                punishment.put("timestamp", rs.getLong("timestamp"));
                punishment.put("expire_timestamp", rs.getLong("expire_timestamp"));
                punishment.put("active", rs.getBoolean("active"));
                punishment.put("removed_by_name", rs.getString("removed_by_name"));
                punishment.put("removal_timestamp", rs.getLong("removal_timestamp"));
                punishment.put("removal_reason", rs.getString("removal_reason"));
                
                history.add(punishment);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player punishment history: " + e.getMessage());
            e.printStackTrace();
        }
        
        return history;
    }
    
    /**
     * Get recent punishments across all players
     * 
     * @param type The punishment type ("BAN", "MUTE", "WARN", "ALL")
     * @param limit Maximum number of items to return
     * @return List of punishment info maps
     */
    public List<Map<String, Object>> getRecentPunishments(String type, int limit) {
        List<Map<String, Object>> punishments = new ArrayList<>();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            StringBuilder query = new StringBuilder();
            
            if ("ALL".equalsIgnoreCase(type) || "BAN".equalsIgnoreCase(type)) {
                // Add bans query
                query.append("SELECT 'BAN' as type, b.id, b.player_uuid, b.player_name, b.banned_by_name as punisher_name, ");
                query.append("b.reason_id, pr.name as reason_name, b.reason as custom_reason, ");
                query.append("b.timestamp, b.expire_timestamp, b.active ");
                query.append("FROM bans b ");
                query.append("LEFT JOIN punishment_reasons pr ON b.reason_id = pr.id ");
                
                if (!"ALL".equalsIgnoreCase(type)) {
                    query.append("ORDER BY b.timestamp DESC LIMIT ?");
                }
            }
            
            if ("ALL".equalsIgnoreCase(type) || "MUTE".equalsIgnoreCase(type)) {
                // Add mutes query
                if (query.length() > 0) {
                    query.append(" UNION ");
                }
                
                query.append("SELECT 'MUTE' as type, m.id, m.player_uuid, m.player_name, m.muted_by_name as punisher_name, ");
                query.append("m.reason_id, pr.name as reason_name, m.reason as custom_reason, ");
                query.append("m.timestamp, m.expire_timestamp, m.active ");
                query.append("FROM mutes m ");
                query.append("LEFT JOIN punishment_reasons pr ON m.reason_id = pr.id ");
                
                if (!"ALL".equalsIgnoreCase(type)) {
                    query.append("ORDER BY m.timestamp DESC LIMIT ?");
                }
            }
            
            if ("ALL".equalsIgnoreCase(type) || "WARN".equalsIgnoreCase(type)) {
                // Add warnings query
                if (query.length() > 0) {
                    query.append(" UNION ");
                }
                
                query.append("SELECT 'WARN' as type, w.id, w.player_uuid, w.player_name, w.warned_by_name as punisher_name, ");
                query.append("w.reason_id, pr.name as reason_name, w.reason as custom_reason, ");
                query.append("w.timestamp, -1 as expire_timestamp, w.active ");
                query.append("FROM warnings w ");
                query.append("LEFT JOIN punishment_reasons pr ON w.reason_id = pr.id ");
                
                if (!"ALL".equalsIgnoreCase(type)) {
                    query.append("ORDER BY w.timestamp DESC LIMIT ?");
                }
            }
            
            if ("ALL".equalsIgnoreCase(type)) {
                // Add the order by and limit clause for combined results
                query.append(" ORDER BY timestamp DESC LIMIT ?");
            }
            
            PreparedStatement ps = conn.prepareStatement(query.toString());
            
            // Set the limit parameter
            ps.setInt(1, limit);
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> punishment = new HashMap<>();
                punishment.put("type", rs.getString("type"));
                punishment.put("id", rs.getInt("id"));
                punishment.put("player_uuid", rs.getString("player_uuid"));
                punishment.put("player_name", rs.getString("player_name"));
                punishment.put("reason_id", rs.getInt("reason_id"));
                punishment.put("reason_name", rs.getString("reason_name"));
                punishment.put("custom_reason", rs.getString("custom_reason"));
                punishment.put("punisher_name", rs.getString("punisher_name"));
                punishment.put("timestamp", rs.getLong("timestamp"));
                punishment.put("expire_timestamp", rs.getLong("expire_timestamp"));
                punishment.put("active", rs.getBoolean("active"));
                
                // Format the duration string
                long expireTimestamp = rs.getLong("expire_timestamp");
                String duration;
                if (expireTimestamp == -1) {
                    duration = configManager.getMessage("punishment.permanent");
                } else if (expireTimestamp == 0) {
                    duration = "N/A";
                } else {
                    long durationMs = expireTimestamp - rs.getLong("timestamp");
                    duration = formatDuration(durationMs);
                }
                punishment.put("duration", duration);
                
                punishments.add(punishment);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get recent punishments: " + e.getMessage());
            e.printStackTrace();
        }
        
        return punishments;
    }
    
    /**
     * Get punishment statistics for the server
     * 
     * @return Map with statistics
     */
    public Map<String, Object> getPunishmentStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Get total bans
            PreparedStatement psBans = conn.prepareStatement(
                    "SELECT COUNT(*) as total, COUNT(*) FILTER (WHERE active = TRUE) as active FROM bans");
            ResultSet rsBans = psBans.executeQuery();
            if (rsBans.next()) {
                stats.put("total_bans", rsBans.getInt("total"));
                stats.put("active_bans", rsBans.getInt("active"));
            }
            rsBans.close();
            psBans.close();
            
            // Get total mutes
            PreparedStatement psMutes = conn.prepareStatement(
                    "SELECT COUNT(*) as total, COUNT(*) FILTER (WHERE active = TRUE) as active FROM mutes");
            ResultSet rsMutes = psMutes.executeQuery();
            if (rsMutes.next()) {
                stats.put("total_mutes", rsMutes.getInt("total"));
                stats.put("active_mutes", rsMutes.getInt("active"));
            }
            rsMutes.close();
            psMutes.close();
            
            // Get total warnings
            PreparedStatement psWarns = conn.prepareStatement(
                    "SELECT COUNT(*) as total, COUNT(*) FILTER (WHERE active = TRUE) as active FROM warnings");
            ResultSet rsWarns = psWarns.executeQuery();
            if (rsWarns.next()) {
                stats.put("total_warnings", rsWarns.getInt("total"));
                stats.put("active_warnings", rsWarns.getInt("active"));
            }
            rsWarns.close();
            psWarns.close();
            
            // Get total reports
            PreparedStatement psReports = conn.prepareStatement(
                    "SELECT COUNT(*) as total, COUNT(*) FILTER (WHERE status = 'OPEN') as active FROM reports");
            ResultSet rsReports = psReports.executeQuery();
            if (rsReports.next()) {
                stats.put("total_reports", rsReports.getInt("total"));
                stats.put("active_reports", rsReports.getInt("active"));
            }
            rsReports.close();
            psReports.close();
            
            // Get top staff by punishments
            PreparedStatement psStaff = conn.prepareStatement(
                    "SELECT staff_name, COUNT(*) as count FROM (" +
                    "    SELECT banned_by_name as staff_name FROM bans " +
                    "    UNION ALL " +
                    "    SELECT muted_by_name FROM mutes " +
                    "    UNION ALL " +
                    "    SELECT warned_by_name FROM warnings" +
                    ") AS all_punishments " +
                    "WHERE staff_name != 'CONSOLE' " +
                    "GROUP BY staff_name " +
                    "ORDER BY count DESC " +
                    "LIMIT 5");
            
            ResultSet rsStaff = psStaff.executeQuery();
            List<Map<String, Object>> topStaff = new ArrayList<>();
            
            while (rsStaff.next()) {
                Map<String, Object> staffEntry = new HashMap<>();
                staffEntry.put("name", rsStaff.getString("staff_name"));
                staffEntry.put("count", rsStaff.getInt("count"));
                topStaff.add(staffEntry);
            }
            
            stats.put("top_staff", topStaff);
            rsStaff.close();
            psStaff.close();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get punishment statistics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * Create a new punishment reason
     * 
     * @param type The punishment type ("BAN", "MUTE", "WARN", "REPORT")
     * @param name The name of the reason
     * @param description Description of the reason
     * @param duration Default duration in milliseconds, -1 for permanent
     * @return ID of the created reason, or -1 if failed
     */
    public int createPunishmentReason(String type, String name, String description, long duration) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Check if a reason with this name already exists for this type
            PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT id FROM punishment_reasons WHERE type = ? AND LOWER(name) = LOWER(?)");
            psCheck.setString(1, type);
            psCheck.setString(2, name);
            
            ResultSet rsCheck = psCheck.executeQuery();
            if (rsCheck.next()) {
                rsCheck.close();
                psCheck.close();
                return -1; // Reason already exists
            }
            
            rsCheck.close();
            psCheck.close();
            
            // Insert the new reason
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO punishment_reasons (type, name, description, duration) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, type);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setLong(4, duration);
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            int reasonId = -1;
            if (rs.next()) {
                reasonId = rs.getInt(1);
                
                // Add to cache
                reasonCache.computeIfAbsent(type, k -> new HashMap<>())
                           .put(name.toLowerCase(), reasonId);
            }
            
            rs.close();
            ps.close();
            
            return reasonId;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create punishment reason: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Delete a punishment reason
     * 
     * @param reasonId ID of the reason to delete
     * @return true if successful
     */
    public boolean deletePunishmentReason(int reasonId) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // First get the reason details for cache update
            PreparedStatement psGet = conn.prepareStatement(
                    "SELECT type, name FROM punishment_reasons WHERE id = ?");
            psGet.setInt(1, reasonId);
            
            ResultSet rs = psGet.executeQuery();
            if (rs.next()) {
                String type = rs.getString("type");
                String name = rs.getString("name").toLowerCase();
                
                // Now delete the reason
                PreparedStatement psDelete = conn.prepareStatement(
                        "DELETE FROM punishment_reasons WHERE id = ?");
                psDelete.setInt(1, reasonId);
                
                int updated = psDelete.executeUpdate();
                psDelete.close();
                
                if (updated > 0) {
                    // Remove from cache
                    Map<String, Integer> typeReasons = reasonCache.get(type);
                    if (typeReasons != null) {
                        typeReasons.remove(name);
                    }
                    
                    rs.close();
                    psGet.close();
                    return true;
                }
            }
            
            rs.close();
            psGet.close();
            return false;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete punishment reason: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if a player can join the server
     * 
     * @param uuid Player UUID
     * @param name Player name
     * @param ip Player IP address
     * @return Ban information if banned, null if not banned
     */
    public Map<String, Object> checkPlayerJoin(UUID uuid, String name, String ip) {
        // First check if the player is banned by UUID
        Map<String, Object> banInfo = getPlayerBan(uuid);
        if (banInfo != null) {
            return banInfo;
        }
        
        // If IP bans are enabled, check for IP ban
        if (plugin.getConfig().getBoolean("punishments.enable_ip_bans", true) && ip != null) {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT b.*, pr.name as reason_name, pr.description as reason_description " +
                        "FROM bans b " +
                        "LEFT JOIN punishment_reasons pr ON b.reason_id = pr.id " +
                        "WHERE b.ip = ? AND b.active = TRUE " + 
                        "AND (b.expire_timestamp > ? OR b.expire_timestamp = -1) " +
                        "ORDER BY b.id DESC LIMIT 1");
                
                ps.setString(1, ip);
                ps.setLong(2, System.currentTimeMillis());
                
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    Map<String, Object> ipBanInfo = new HashMap<>();
                    ipBanInfo.put("id", rs.getInt("id"));
                    ipBanInfo.put("reason_id", rs.getInt("reason_id"));
                    ipBanInfo.put("reason_name", rs.getString("reason_name"));
                    ipBanInfo.put("reason_description", rs.getString("reason_description"));
                    ipBanInfo.put("custom_reason", rs.getString("reason"));
                    ipBanInfo.put("banned_by", rs.getString("banned_by_name"));
                    ipBanInfo.put("timestamp", rs.getLong("timestamp"));
                    ipBanInfo.put("expire_timestamp", rs.getLong("expire_timestamp"));
                    ipBanInfo.put("is_ip_ban", true);
                    
                    rs.close();
                    ps.close();
                    
                    return ipBanInfo;
                }
                
                rs.close();
                ps.close();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to check IP ban: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // No ban found
        return null;
    }
    
    /**
     * Clean up expired punishments
     */
    public void cleanupExpiredPunishments() {
        long now = System.currentTimeMillis();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Update expired bans
            PreparedStatement psBans = conn.prepareStatement(
                    "UPDATE bans SET active = FALSE, unban_timestamp = ?, unbanned_by = 'SYSTEM', " +
                    "unbanned_by_name = 'System', unban_reason = 'Expired' " +
                    "WHERE active = TRUE AND expire_timestamp > 0 AND expire_timestamp <= ?");
            
            psBans.setLong(1, now);
            psBans.setLong(2, now);
            int expiredBans = psBans.executeUpdate();
            psBans.close();
            
            // Update expired mutes
            PreparedStatement psMutes = conn.prepareStatement(
                    "UPDATE mutes SET active = FALSE, unmute_timestamp = ?, unmuted_by = 'SYSTEM', " +
                    "unmuted_by_name = 'System', unmute_reason = 'Expired' " +
                    "WHERE active = TRUE AND expire_timestamp > 0 AND expire_timestamp <= ?");
            
            psMutes.setLong(1, now);
            psMutes.setLong(2, now);
            int expiredMutes = psMutes.executeUpdate();
            psMutes.close();
            
            if (expiredBans > 0 || expiredMutes > 0) {
                plugin.getLogger().info("Cleaned up expired punishments: " + expiredBans + " bans, " + 
                        expiredMutes + " mutes");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clean up expired punishments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Schedule regular cleanup of expired punishments
     */
    public void scheduleCleanupTask() {
        int interval = plugin.getConfig().getInt("punishments.cleanup_interval_minutes", 5);
        
        ProxyServer.getInstance().getScheduler().schedule(plugin, this::cleanupExpiredPunishments, 
                1, interval * 60, java.util.concurrent.TimeUnit.SECONDS);
        
        plugin.getLogger().info("Scheduled punishment cleanup task to run every " + interval + " minutes");
    }
} 