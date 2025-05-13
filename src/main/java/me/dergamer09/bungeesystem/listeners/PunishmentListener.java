package me.dergamer09.bungeesystem.listeners;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.PunishmentManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Listener for punishment-related events
 */
public class PunishmentListener implements Listener {

    private final BungeeSystem plugin;
    private final PunishmentManager punishmentManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public PunishmentListener(BungeeSystem plugin) {
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }
    
    /**
     * Check if player is banned on login
     */
    @EventHandler
    public void onPlayerPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        
        // Check for active warnings to display later
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            // Make sure player is still online before checking warnings
            if (player.isConnected()) {
                showWarningsToPlayer(player);
            }
        }, 5, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    /**
     * Show active warnings to a player
     */
    private void showWarningsToPlayer(ProxiedPlayer player) {
        List<Map<String, Object>> warnings = punishmentManager.getPlayerWarnings(player.getUniqueId());
        
        if (warnings.isEmpty()) {
            return;
        }
        
        player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You have " + 
                warnings.size() + " active warning" + (warnings.size() > 1 ? "s" : "") + ":"));
        
        for (Map<String, Object> warning : warnings) {
            int id = (int) warning.get("id");
            String reasonName = (String) warning.get("reason_name");
            String customReason = (String) warning.get("custom_reason");
            String warnedBy = (String) warning.get("warned_by");
            long timestamp = (long) warning.get("timestamp");
            
            // Format the full reason
            String reason = reasonName;
            if (customReason != null && !customReason.isEmpty()) {
                reason += ": " + customReason;
            }
            
            // Format date
            String dateStr = dateFormat.format(new Date(timestamp));
            
            // Create clickable warning message
            TextComponent message = new TextComponent(ChatColor.YELLOW + "• " + ChatColor.RED + reason);
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                    ChatColor.GOLD + "Warning #" + id + "\n" +
                    ChatColor.WHITE + "Reason: " + ChatColor.RED + reason + "\n" +
                    ChatColor.WHITE + "Warned by: " + ChatColor.YELLOW + warnedBy + "\n" +
                    ChatColor.WHITE + "Date: " + dateStr
            )));
            
            player.sendMessage(message);
        }
        
        // Add cautionary message
        player.sendMessage(new TextComponent(ChatColor.RED + "Please follow the server rules to avoid further punishment."));
        
        // Show information about auto-punishments if configured
        int warnsToBan = plugin.getConfig().getInt("punishments.warnings_to_ban", -1);
        int warnsToMute = plugin.getConfig().getInt("punishments.warnings_to_mute", -1);
        
        if (warnsToBan > 0 || warnsToMute > 0) {
            TextComponent autoPunishMsg = new TextComponent(ChatColor.GRAY + "Note: ");
            
            if (warnsToBan > 0) {
                autoPunishMsg.addExtra(new TextComponent(ChatColor.GRAY + "Reaching " + warnsToBan + 
                        " warnings will result in an automatic ban."));
            }
            
            if (warnsToMute > 0) {
                if (warnsToBan > 0) {
                    autoPunishMsg.addExtra(new TextComponent(" "));
                }
                autoPunishMsg.addExtra(new TextComponent(ChatColor.GRAY + "Reaching " + warnsToMute + 
                        " warnings will result in an automatic mute."));
            }
            
            player.sendMessage(autoPunishMsg);
        }
    }
} 