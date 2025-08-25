package me.dergamer09.bungeesystem.velocity.Managers;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player punishments (bans, mutes, warnings) and reports for Velocity
 */
public class PunishmentManager {

    private final VelocitySystem plugin;
    private final ConfigManager configManager;
    private final ProxyServer server;
    private final Logger logger;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    
    // Cache reason IDs to reduce database queries
    private final Map<String, Map<String, Integer>> reasonCache = new HashMap<>();

    public PunishmentManager(VelocitySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.server = plugin.getServer();
        this.logger = plugin.getLogger();
        
        // Initialize reason cache
        initReasonCache();
    }
    
    /**
     * Initialize the cache of punishment reason IDs
     */
    private void initReasonCache() {
        server.getScheduler().buildTask(plugin, () -> {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                logger.error("Failed to initialize punishment reason cache: Database connection is null");
                return;
            }
            
            try {
                PreparedStatement ps = conn.prepareStatement("SELECT type, name, id FROM punishment_reasons ORDER BY type, id");
                ResultSet rs = ps.executeQuery();
                
                while (rs.next()) {
                    String type = rs.getString("type");
                    String name = rs.getString("name");
                    int id = rs.getInt("id");
                    
                    reasonCache.computeIfAbsent(type, k -> new HashMap<>()).put(name.toLowerCase(), id);
                }
                
                rs.close();
                ps.close();
                logger.info("Loaded {} punishment reason types into cache", reasonCache.size());
                
            } catch (SQLException e) {
                logger.error("Failed to load punishment reasons: " + e.getMessage());
            }
        }).schedule();
    }
    
    /**
     * Get reason ID by type and name
     */
    public int getReasonId(String type, String reasonName) {
        Map<String, Integer> typeReasons = reasonCache.get(type);
        if (typeReasons == null) {
            return -1;
        }
        return typeReasons.getOrDefault(reasonName.toLowerCase(), -1);
    }
    
    /**
     * Get all reasons for a specific type
     */
    public List<Map<String, Object>> getReasons(String type) {
        List<Map<String, Object>> reasons = new ArrayList<>();
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return reasons;
            
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, description, duration FROM punishment_reasons WHERE type = ? ORDER BY id");
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
            logger.error("Failed to get reasons for type " + type + ": " + e.getMessage());
        }
        
        return reasons;
    }
    
    /**
     * Get reason name by ID
     */
    public String getReasonName(int reasonId) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return "Unknown";
            
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM punishment_reasons WHERE id = ?");
            ps.setInt(1, reasonId);
            ResultSet rs = ps.executeQuery();
            
            String name = "Unknown";
            if (rs.next()) {
                name = rs.getString("name");
            }
            
            rs.close();
            ps.close();
            return name;
            
        } catch (SQLException e) {
            logger.error("Failed to get reason name for ID " + reasonId + ": " + e.getMessage());
            return "Unknown";
        }
    }
    
    /**
     * Mute a player
     */
    public boolean mutePlayer(CommandSource sender, String targetName, int reasonId, String customReason, long duration) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                sender.sendMessage(Component.text(configManager.getMessage("general.database_error")));
                return false;
            }
            
            // Get target UUID
            UUID targetUuid = plugin.getDatabaseManager().getUUIDFromName(targetName);
            if (targetUuid == null) {
                String message = configManager.getMessage("system.player_not_found", "player", targetName);
                sender.sendMessage(Component.text(message));
                return false;
            }
            
            // Check if already muted
            if (isPlayerMuted(targetUuid)) {
                String message = configManager.getMessage("punishment.mute.already_muted", "player", targetName);
                sender.sendMessage(Component.text(message));
                return false;
            }
            
            // Calculate expiration
            long expirationTime = duration > 0 ? System.currentTimeMillis() + (duration * 1000) : -1;
            
            // Insert mute record
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO punishments (player_uuid, player_name, type, reason_id, custom_reason, " +
                "punisher_uuid, punisher_name, timestamp, expiration, active) VALUES (?, ?, 'MUTE', ?, ?, ?, ?, ?, ?, 1)");
            
            ps.setString(1, targetUuid.toString());
            ps.setString(2, targetName);
            ps.setInt(3, reasonId);
            ps.setString(4, customReason);
            
            String punisherUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
            String punisherName = sender instanceof Player ? ((Player) sender).getUsername() : "CONSOLE";
            ps.setString(5, punisherUuid);
            ps.setString(6, punisherName);
            
            ps.setLong(7, System.currentTimeMillis());
            ps.setLong(8, expirationTime);
            
            int result = ps.executeUpdate();
            ps.close();
            
            if (result > 0) {
                String message = configManager.getMessage("punishment.mute.success", "player", targetName);
                sender.sendMessage(Component.text(message));
                
                // Notify target player if online
                server.getPlayer(targetName).ifPresent(target -> {
                    String reasonName = getReasonName(reasonId);
                    String fullReason = customReason != null && !customReason.isEmpty() ? 
                            reasonName + ": " + customReason : reasonName;
                    String expire = duration > 0 ? dateFormat.format(new Date(expirationTime)) : "Permanent";
                    
                    String muteMsg = configManager.getMessage("punishment.mute_message", 
                            "reason", fullReason, "expire", expire);
                    target.sendMessage(Component.text(muteMsg));
                });
                
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to mute player " + targetName + ": " + e.getMessage());
            String message = configManager.getMessage("general.database_error");
            sender.sendMessage(Component.text(message));
        }
        
        return false;
    }
    
    /**
     * Ban a player
     */
    public boolean banPlayer(CommandSource sender, String targetName, int reasonId, String customReason, long duration) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                sender.sendMessage(Component.text(configManager.getMessage("general.database_error")));
                return false;
            }
            
            // Get target UUID
            UUID targetUuid = plugin.getDatabaseManager().getUUIDFromName(targetName);
            if (targetUuid == null) {
                String message = configManager.getMessage("system.player_not_found", "player", targetName);
                sender.sendMessage(Component.text(message));
                return false;
            }
            
            // Check if already banned
            if (isPlayerBanned(targetUuid)) {
                String message = configManager.getMessage("punishment.ban.already_banned", "player", targetName);
                sender.sendMessage(Component.text(message));
                return false;
            }
            
            // Calculate expiration
            long expirationTime = duration > 0 ? System.currentTimeMillis() + (duration * 1000) : -1;
            
            // Insert ban record
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO punishments (player_uuid, player_name, type, reason_id, custom_reason, " +
                "punisher_uuid, punisher_name, timestamp, expiration, active) VALUES (?, ?, 'BAN', ?, ?, ?, ?, ?, ?, 1)");
            
            ps.setString(1, targetUuid.toString());
            ps.setString(2, targetName);
            ps.setInt(3, reasonId);
            ps.setString(4, customReason);
            
            String punisherUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
            String punisherName = sender instanceof Player ? ((Player) sender).getUsername() : "CONSOLE";
            ps.setString(5, punisherUuid);
            ps.setString(6, punisherName);
            
            ps.setLong(7, System.currentTimeMillis());
            ps.setLong(8, expirationTime);
            
            int result = ps.executeUpdate();
            ps.close();
            
            if (result > 0) {
                String message = configManager.getMessage("punishment.ban.success", "player", targetName);
                sender.sendMessage(Component.text(message));
                
                // Kick player if online
                server.getPlayer(targetName).ifPresent(target -> {
                    String reasonName = getReasonName(reasonId);
                    String fullReason = customReason != null && !customReason.isEmpty() ? 
                            reasonName + ": " + customReason : reasonName;
                    String expire = duration > 0 ? dateFormat.format(new Date(expirationTime)) : "Permanent";
                    
                    String banMsg = configManager.getMessage("punishment.ban_kick_message", 
                            "reason", fullReason, "expire", expire);
                    target.disconnect(Component.text(banMsg));
                });
                
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to ban player " + targetName + ": " + e.getMessage());
            String message = configManager.getMessage("general.database_error");
            sender.sendMessage(Component.text(message));
        }
        
        return false;
    }
    
    /**
     * Kick a player
     */
    public boolean kickPlayer(CommandSource sender, String targetName, int reasonId, String customReason) {
        // Get target player
        Player target = server.getPlayer(targetName).orElse(null);
        if (target == null) {
            String message = configManager.getMessage("system.player_not_found", "player", targetName);
            sender.sendMessage(Component.text(message));
            return false;
        }
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                sender.sendMessage(Component.text(configManager.getMessage("general.database_error")));
                return false;
            }
            
            // Insert kick record
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO punishments (player_uuid, player_name, type, reason_id, custom_reason, " +
                "punisher_uuid, punisher_name, timestamp, expiration, active) VALUES (?, ?, 'KICK', ?, ?, ?, ?, ?, -1, 0)");
            
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, targetName);
            ps.setInt(3, reasonId);
            ps.setString(4, customReason);
            
            String punisherUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
            String punisherName = sender instanceof Player ? ((Player) sender).getUsername() : "CONSOLE";
            ps.setString(5, punisherUuid);
            ps.setString(6, punisherName);
            
            ps.setLong(7, System.currentTimeMillis());
            
            int result = ps.executeUpdate();
            ps.close();
            
            if (result > 0) {
                String message = configManager.getMessage("punishment.kick.success", "player", targetName);
                sender.sendMessage(Component.text(message));
                
                // Kick the player
                String reasonName = getReasonName(reasonId);
                String fullReason = customReason != null && !customReason.isEmpty() ? 
                        reasonName + ": " + customReason : reasonName;
                
                target.disconnect(Component.text("You have been kicked!\nReason: " + fullReason));
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to kick player " + targetName + ": " + e.getMessage());
            String message = configManager.getMessage("general.database_error");
            sender.sendMessage(Component.text(message));
        }
        
        return false;
    }
    
    /**
     * Warn a player
     */
    public boolean warnPlayer(CommandSource sender, String targetName, int reasonId, String customReason) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                sender.sendMessage(Component.text(configManager.getMessage("general.database_error")));
                return false;
            }
            
            // Get target UUID
            UUID targetUuid = plugin.getDatabaseManager().getUUIDFromName(targetName);
            if (targetUuid == null) {
                String message = configManager.getMessage("system.player_not_found", "player", targetName);
                sender.sendMessage(Component.text(message));
                return false;
            }
            
            // Insert warning record
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO punishments (player_uuid, player_name, type, reason_id, custom_reason, " +
                "punisher_uuid, punisher_name, timestamp, expiration, active) VALUES (?, ?, 'WARN', ?, ?, ?, ?, ?, -1, 1)");
            
            ps.setString(1, targetUuid.toString());
            ps.setString(2, targetName);
            ps.setInt(3, reasonId);
            ps.setString(4, customReason);
            
            String punisherUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
            String punisherName = sender instanceof Player ? ((Player) sender).getUsername() : "CONSOLE";
            ps.setString(5, punisherUuid);
            ps.setString(6, punisherName);
            
            ps.setLong(7, System.currentTimeMillis());
            
            int result = ps.executeUpdate();
            ps.close();
            
            if (result > 0) {
                String message = configManager.getMessage("punishment.warn.success", "player", targetName);
                sender.sendMessage(Component.text(message));
                
                // Notify target player if online
                server.getPlayer(targetName).ifPresent(target -> {
                    String reasonName = getReasonName(reasonId);
                    String fullReason = customReason != null && !customReason.isEmpty() ? 
                            reasonName + ": " + customReason : reasonName;
                    
                    target.sendMessage(Component.text("§cYou have been warned!\n§7Reason: §f" + fullReason));
                });
                
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to warn player " + targetName + ": " + e.getMessage());
            String message = configManager.getMessage("general.database_error");
            sender.sendMessage(Component.text(message));
        }
        
        return false;
    }
    
    /**
     * Check if a player is currently muted
     */
    public boolean isPlayerMuted(UUID playerUuid) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return false;
            
            PreparedStatement ps = conn.prepareStatement(
                "SELECT expiration FROM punishments WHERE player_uuid = ? AND type = 'MUTE' AND active = 1 " +
                "AND (expiration = -1 OR expiration > ?) ORDER BY timestamp DESC LIMIT 1");
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            
            ResultSet rs = ps.executeQuery();
            boolean muted = rs.next();
            
            rs.close();
            ps.close();
            return muted;
            
        } catch (SQLException e) {
            logger.error("Failed to check mute status for UUID " + playerUuid + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a player is currently banned
     */
    public boolean isPlayerBanned(UUID playerUuid) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return false;
            
            PreparedStatement ps = conn.prepareStatement(
                "SELECT expiration FROM punishments WHERE player_uuid = ? AND type = 'BAN' AND active = 1 " +
                "AND (expiration = -1 OR expiration > ?) ORDER BY timestamp DESC LIMIT 1");
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            
            ResultSet rs = ps.executeQuery();
            boolean banned = rs.next();
            
            rs.close();
            ps.close();
            return banned;
            
        } catch (SQLException e) {
            logger.error("Failed to check ban status for UUID " + playerUuid + ": " + e.getMessage());
            return false;
        }
    }
}
