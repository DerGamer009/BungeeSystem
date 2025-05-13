package me.dergamer09.bungeesystem.listeners;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ChatManager;
import me.dergamer09.bungeesystem.Managers.PunishmentManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Listener for chat events to handle global chat and formatting
 */
public class ChatListener implements Listener {

    private final BungeeSystem plugin;
    private final ChatManager chatManager;
    private final PunishmentManager punishmentManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    
    public ChatListener(BungeeSystem plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
        this.punishmentManager = plugin.getPunishmentManager();
    }
    
    /**
     * Handle chat messages
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(ChatEvent event) {
        // Ignore commands and cancelled events
        if (event.isCommand() || event.isCancelled()) {
            return;
        }
        
        // Make sure sender is a player
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        String message = event.getMessage();
        
        // Check if player is muted
        if (punishmentManager.isPlayerMuted(player.getUniqueId())) {
            // Get mute details
            Map<String, Object> muteInfo = punishmentManager.getPlayerMute(player.getUniqueId());
            
            if (muteInfo != null) {
                // Format expiry time for display
                String expiryStr;
                long expireTimestamp = (long) muteInfo.get("expire_timestamp");
                
                if (expireTimestamp < 0) {
                    expiryStr = "permanent";
                } else {
                    expiryStr = "until " + dateFormat.format(new Date(expireTimestamp));
                }
                
                // Get reason information
                String reasonName = (String) muteInfo.get("reason_name");
                String customReason = (String) muteInfo.get("custom_reason");
                String mutedBy = (String) muteInfo.get("muted_by");
                
                // Format the full reason
                String reason = reasonName;
                if (customReason != null && !customReason.isEmpty()) {
                    reason += ": " + customReason;
                }
                
                // Notify the player they are muted
                player.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                        "You are muted " + expiryStr + " for: " + reason));
                
                // Cancel the chat message
                event.setCancelled(true);
                return;
            }
        }
        
        // Check if player has global chat enabled
        if (chatManager.hasGlobalChatEnabled(player)) {
            // Cancel the original event to prevent the message from being sent to the server
            event.setCancelled(true);
            
            // Send message to all players with global chat formatting
            chatManager.sendGlobalChatMessage(player, message);
        }
    }
} 