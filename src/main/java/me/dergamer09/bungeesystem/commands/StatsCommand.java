package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import me.dergamer09.bungeesystem.Managers.StatsManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class StatsCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public StatsCommand() {
        super("stats", "bungeesystem.stats.use");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer target;
        
        if (args.length == 0) {
            // No arguments, show stats for the sender if it's a player
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(new TextComponent(configManager.getMessage("stats.console_must_specify_player")));
                return;
            }
            target = (ProxiedPlayer) sender;
        } else {
            // Try to find the specified player
            String playerName = args[0];
            target = ProxyServer.getInstance().getPlayer(playerName);
            
            // If player is not online, try to get stats from database
            if (target == null) {
                lookupOfflinePlayer(sender, playerName);
                return;
            }
        }
        
        // Display stats for the online player
        displayPlayerStats(sender, target.getUniqueId(), target.getName());
    }
    
    /**
     * Look up stats for an offline player
     * 
     * @param sender The command sender
     * @param playerName The name of the player to look up
     */
    private void lookupOfflinePlayer(CommandSender sender, String playerName) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                // Try to find the player's UUID in the database
                UUID uuid = plugin.getDatabaseManager().getUUIDFromName(playerName);
                
                if (uuid == null) {
                    sender.sendMessage(new TextComponent(
                            configManager.getMessage("stats.player_not_found", "player", playerName)));
                    return;
                }
                
                // Display stats for the offline player
                displayPlayerStats(sender, uuid, playerName);
                
            } catch (Exception e) {
                sender.sendMessage(new TextComponent(
                        configManager.getMessage("stats.database_error", "error", e.getMessage())));
                plugin.getLogger().severe("Error in StatsCommand: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Display player statistics
     * 
     * @param sender The command sender
     * @param uuid The UUID of the player
     * @param playerName The name of the player
     */
    private void displayPlayerStats(CommandSender sender, UUID uuid, String playerName) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            StatsManager statsManager = plugin.getStatsManager();
            StatsManager.PlayerStats stats = statsManager.getPlayerStats(uuid);
            
            if (stats == null) {
                sender.sendMessage(new TextComponent(
                        configManager.getMessage("stats.no_stats_found", "player", playerName)));
                return;
            }
            
            // Send stats header
            sender.sendMessage(new TextComponent(configManager.getMessage("stats.header", "player", playerName)));
            
            // First join date
            if (stats.getFirstJoin() > 0) {
                String formattedDate = dateFormat.format(new Date(stats.getFirstJoin()));
                sender.sendMessage(new TextComponent(configManager.getMessage("stats.first_joined", "date", formattedDate)));
            }
            
            // Last seen date (if player is offline)
            if (ProxyServer.getInstance().getPlayer(uuid) == null && stats.getLastJoin() > 0) {
                String formattedDate = dateFormat.format(new Date(stats.getLastJoin()));
                sender.sendMessage(new TextComponent(configManager.getMessage("stats.last_seen", "date", formattedDate)));
            }
            
            // Total online time
            String onlineTime = StatsManager.formatTimeDuration(stats.getTotalOnlineTime());
            sender.sendMessage(new TextComponent(configManager.getMessage("stats.online_time", "time", onlineTime)));
            
            // Login count
            sender.sendMessage(new TextComponent(configManager.getMessage("stats.login_count", 
                    "count", String.valueOf(stats.getLoginCount()))));
            
            // Votes
            sender.sendMessage(new TextComponent(configManager.getMessage("stats.votes", 
                    "count", String.valueOf(stats.getVotes()))));
            
            // Messages sent
            sender.sendMessage(new TextComponent(configManager.getMessage("stats.messages", 
                    "count", String.valueOf(stats.getMessagesSent()))));
            
            // Footer
            sender.sendMessage(new TextComponent(configManager.getMessage("stats.footer")));
        });
    }
} 