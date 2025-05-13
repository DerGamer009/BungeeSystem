package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages team chat channels and global chat functionality
 */
public class ChatManager {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private LuckPerms luckPermsApi;
    private boolean luckPermsEnabled = false;
    
    // Chat channels for team chat
    public static final String ADMIN_CHANNEL = "admin";
    public static final String SUPPORT_CHANNEL = "support";
    public static final String DEV_CHANNEL = "dev";
    public static final String MOD_CHANNEL = "mod";
    public static final String TEAM_CHANNEL = "team"; // Default channel for all team members
    
    // Map to store which players are in which channel
    private final Map<UUID, String> playerChannels = new HashMap<>();
    
    // Players who have enabled global chat mode
    private final Set<UUID> globalChatEnabled = new HashSet<>();
    
    // Pattern for hex color codes (e.g., #FFFFFF)
    private final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
    
    /**
     * Creates a new ChatManager
     * 
     * @param plugin The BungeeSystem instance
     */
    public ChatManager(BungeeSystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        
        // Setup LuckPerms integration
        setupLuckPerms();
    }
    
    /**
     * Setup LuckPerms integration
     */
    private void setupLuckPerms() {
        try {
            // Try to get LuckPerms API
            if (ProxyServer.getInstance().getPluginManager().getPlugin("LuckPerms") != null) {
                luckPermsApi = LuckPermsProvider.get();
                luckPermsEnabled = true;
                plugin.getLogger().info("LuckPerms integration enabled for chat formatting.");
            } else {
                plugin.getLogger().warning("LuckPerms not found. Using default chat formatting.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting up LuckPerms integration: " + e.getMessage());
        }
    }
    
    /**
     * Send a message to a team chat channel
     * 
     * @param sender The sender of the message
     * @param channel The channel to send the message to
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean sendTeamChatMessage(CommandSender sender, String channel, String message) {
        // Check if sender has permission for the specified channel
        if (!hasChannelPermission(sender, channel)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.no_permission")));
            return false;
        }
        
        // Format message with prefix and sender name
        String formattedMessage = formatTeamChatMessage(sender, channel, message);
        
        // Send to all players with permission to see this channel
        int recipientCount = 0;
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (hasChannelPermission(player, channel)) {
                player.sendMessage(new TextComponent(formattedMessage));
                recipientCount++;
            }
        }
        
        // Also send to console
        ProxyServer.getInstance().getConsole().sendMessage(new TextComponent(formattedMessage));
        
        // Update player's active channel if they are a player
        if (sender instanceof ProxiedPlayer) {
            playerChannels.put(((ProxiedPlayer) sender).getUniqueId(), channel);
        }
        
        // Return true if at least one recipient received the message (other than the sender)
        return recipientCount > (sender instanceof ProxiedPlayer ? 1 : 0);
    }
    
    /**
     * Format a team chat message
     * 
     * @param sender The sender of the message
     * @param channel The channel the message is being sent to
     * @param message The message content
     * @return The formatted message
     */
    private String formatTeamChatMessage(CommandSender sender, String channel, String message) {
        String senderName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        String prefix = getChannelPrefix(channel);
        return prefix + " " + senderName + ": " + ChatColor.WHITE + message;
    }
    
    /**
     * Get the prefix for a channel
     * 
     * @param channel The channel
     * @return The prefix including color
     */
    private String getChannelPrefix(String channel) {
        switch (channel.toLowerCase()) {
            case ADMIN_CHANNEL:
                return ChatColor.RED + "[Admin Chat]";
            case SUPPORT_CHANNEL:
                return ChatColor.GOLD + "[Support Chat]";
            case DEV_CHANNEL:
                return ChatColor.AQUA + "[Dev Chat]";
            case MOD_CHANNEL:
                return ChatColor.GREEN + "[Mod Chat]";
            case TEAM_CHANNEL:
            default:
                return ChatColor.YELLOW + "[Team Chat]";
        }
    }
    
    /**
     * Check if a sender has permission for a specified channel
     * 
     * @param sender The sender to check
     * @param channel The channel to check for
     * @return true if the sender has permission, false otherwise
     */
    public boolean hasChannelPermission(CommandSender sender, String channel) {
        if (sender == ProxyServer.getInstance().getConsole()) {
            return true; // Console has access to all channels
        }
        
        String permission;
        switch (channel.toLowerCase()) {
            case ADMIN_CHANNEL:
                permission = "bungeesystem.teamchat.admin";
                break;
            case SUPPORT_CHANNEL:
                permission = "bungeesystem.teamchat.support";
                break;
            case DEV_CHANNEL:
                permission = "bungeesystem.teamchat.dev";
                break;
            case MOD_CHANNEL:
                permission = "bungeesystem.teamchat.mod";
                break;
            case TEAM_CHANNEL:
            default:
                permission = "bungeesystem.teamchat.use";
                break;
        }
        
        return sender.hasPermission(permission);
    }
    
    /**
     * Get a player's active team chat channel
     * 
     * @param player The player
     * @return The channel, or null if not in a channel
     */
    public String getPlayerChannel(ProxiedPlayer player) {
        return playerChannels.getOrDefault(player.getUniqueId(), TEAM_CHANNEL);
    }
    
    /**
     * Set a player's active team chat channel
     * 
     * @param player The player
     * @param channel The channel to set
     * @return true if successful, false if player lacks permission
     */
    public boolean setPlayerChannel(ProxiedPlayer player, String channel) {
        if (!hasChannelPermission(player, channel)) {
            return false;
        }
        
        playerChannels.put(player.getUniqueId(), channel);
        return true;
    }
    
    /**
     * Toggle global chat mode for a player
     * 
     * @param player The player
     * @return true if global chat is now enabled, false if disabled
     */
    public boolean toggleGlobalChat(ProxiedPlayer player) {
        UUID uuid = player.getUniqueId();
        
        if (globalChatEnabled.contains(uuid)) {
            globalChatEnabled.remove(uuid);
            return false;
        } else {
            globalChatEnabled.add(uuid);
            return true;
        }
    }
    
    /**
     * Check if a player has global chat enabled
     * 
     * @param player The player
     * @return true if global chat is enabled
     */
    public boolean hasGlobalChatEnabled(ProxiedPlayer player) {
        return globalChatEnabled.contains(player.getUniqueId());
    }
    
    /**
     * Send a global chat message to all players on the network
     * 
     * @param player The sender
     * @param message The message
     */
    public void sendGlobalChatMessage(ProxiedPlayer player, String message) {
        String formattedMessage = formatGlobalChatMessage(player, message);
        
        // Send to all players on the network
        for (ProxiedPlayer recipient : ProxyServer.getInstance().getPlayers()) {
            recipient.sendMessage(new TextComponent(formattedMessage));
        }
        
        // Send to console
        ProxyServer.getInstance().getConsole().sendMessage(new TextComponent(formattedMessage));
    }
    
    /**
     * Format a global chat message with LuckPerms prefix/suffix
     * 
     * @param player The sender
     * @param message The message
     * @return The formatted message
     */
    private String formatGlobalChatMessage(ProxiedPlayer player, String message) {
        if (luckPermsEnabled) {
            return formatWithLuckPerms(player, message);
        } else {
            return formatDefault(player, message);
        }
    }
    
    /**
     * Format a chat message using LuckPerms prefix and suffix
     * 
     * @param player The sender
     * @param message The message
     * @return The formatted message
     */
    private String formatWithLuckPerms(ProxiedPlayer player, String message) {
        try {
            User user = luckPermsApi.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return formatDefault(player, message);
            }
            
            // Get prefix and suffix from LuckPerms
            String prefix = user.getCachedData().getMetaData().getPrefix();
            String suffix = user.getCachedData().getMetaData().getSuffix();
            
            prefix = prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : "";
            suffix = suffix != null ? ChatColor.translateAlternateColorCodes('&', suffix) : "";
            
            // Apply color codes to message if player has permission
            if (player.hasPermission("bungeesystem.chat.color")) {
                message = ChatColor.translateAlternateColorCodes('&', message);
                
                // Process hex colors if player has permission
                if (player.hasPermission("bungeesystem.chat.hex")) {
                    message = processHexColors(message);
                }
            }
            
            // Get player's group for formatting
            String groupName = "default";
            String groupDisplayName = "Default";
            
            String primaryGroup = user.getPrimaryGroup();
            if (primaryGroup != null && !primaryGroup.isEmpty()) {
                Group group = luckPermsApi.getGroupManager().getGroup(primaryGroup);
                if (group != null) {
                    groupName = primaryGroup;
                    String displayName = group.getDisplayName();
                    groupDisplayName = displayName != null ? displayName : primaryGroup;
                }
            }
            
            // Format using the player's server name
            String serverName = player.getServer().getInfo().getName();
            
            // Construct the message with format from config
            Configuration config = plugin.getConfig();
            String format = config.getString("chat.format", "%prefix%&r %player% &7» %message%");
            
            return format
                    .replace("%prefix%", prefix)
                    .replace("%suffix%", suffix)
                    .replace("%player%", player.getName())
                    .replace("%displayname%", player.getDisplayName())
                    .replace("%server%", serverName)
                    .replace("%group%", groupName)
                    .replace("%groupdisplay%", groupDisplayName)
                    .replace("%message%", message);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting with LuckPerms: " + e.getMessage());
            return formatDefault(player, message);
        }
    }
    
    /**
     * Format a chat message with default formatting
     * 
     * @param player The sender
     * @param message The message
     * @return The formatted message
     */
    private String formatDefault(ProxiedPlayer player, String message) {
        // Apply color codes if player has permission
        if (player.hasPermission("bungeesystem.chat.color")) {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }
        
        Configuration config = plugin.getConfig();
        String globalPrefix = config.getString("chat.global_prefix", "&6[G]");
        globalPrefix = ChatColor.translateAlternateColorCodes('&', globalPrefix);
        
        String serverName = player.getServer().getInfo().getName();
        return globalPrefix + " &7[" + serverName + "] &f" + player.getName() + "&7: &f" + message;
    }
    
    /**
     * Process hex color codes in a message (e.g., #FFFFFF)
     * 
     * @param message The message with hex codes
     * @return The message with translated hex codes
     */
    private String processHexColors(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        
        while (matcher.find()) {
            String group = matcher.group();
            matcher.appendReplacement(buffer, ChatColor.of(group).toString());
        }
        
        return matcher.appendTail(buffer).toString();
    }
} 