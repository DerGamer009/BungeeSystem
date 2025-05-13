package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkCommand extends Command {

    private static final Map<UUID, Boolean> afkPlayers = new HashMap<>();
    private final BungeeSystem plugin;
    private final ConfigManager configManager;

    public AfkCommand() {
        super("afk", "bungeesystem.afk");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("general.player_only")));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        UUID uuid = player.getUniqueId();
        
        // Toggle AFK status
        boolean isAfk = !isAfk(uuid);
        afkPlayers.put(uuid, isAfk);
        
        if (isAfk) {
            // Player is now AFK
            ProxyServer.getInstance().broadcast(new TextComponent(
                    configManager.getMessage("afk.player_now_afk", "player", player.getName())));
            
            // If player has display name set, prefix it with AFK
            String displayName = player.getDisplayName();
            if (!displayName.startsWith(ChatColor.GRAY + "[AFK] ")) {
                player.setDisplayName(ChatColor.GRAY + "[AFK] " + displayName);
            }
        } else {
            // Player is no longer AFK
            ProxyServer.getInstance().broadcast(new TextComponent(
                    configManager.getMessage("afk.player_no_longer_afk", "player", player.getName())));
            
            // Remove AFK prefix from display name
            String displayName = player.getDisplayName();
            if (displayName.startsWith(ChatColor.GRAY + "[AFK] ")) {
                player.setDisplayName(displayName.substring((ChatColor.GRAY + "[AFK] ").length()));
            }
        }
    }

    /**
     * Checks if a player is currently AFK
     * @param uuid The UUID of the player to check
     * @return true if the player is AFK, false otherwise
     */
    public static boolean isAfk(UUID uuid) {
        return afkPlayers.getOrDefault(uuid, false);
    }
    
    /**
     * Sets a player's AFK status directly
     * @param uuid The UUID of the player
     * @param afk The AFK status to set
     */
    public static void setAfk(UUID uuid, boolean afk) {
        afkPlayers.put(uuid, afk);
        
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null && player.isConnected()) {
            String displayName = player.getDisplayName();
            
            if (afk && !displayName.startsWith(ChatColor.GRAY + "[AFK] ")) {
                player.setDisplayName(ChatColor.GRAY + "[AFK] " + displayName);
            } else if (!afk && displayName.startsWith(ChatColor.GRAY + "[AFK] ")) {
                player.setDisplayName(displayName.substring((ChatColor.GRAY + "[AFK] ").length()));
            }
        }
    }
    
    /**
     * Clears the AFK status when a player disconnects
     * @param uuid The UUID of the player
     */
    public static void clearAfk(UUID uuid) {
        afkPlayers.remove(uuid);
    }
} 