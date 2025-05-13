package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import me.dergamer09.bungeesystem.Managers.StatsManager;
import me.dergamer09.bungeesystem.Managers.StatsManager.PlayerStats;
import me.dergamer09.bungeesystem.Managers.StatsManager.StatType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;

public class TopCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private final int DEFAULT_LIMIT = 10;

    public TopCommand() {
        super("top", "bungeesystem.top.use");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        StatType statType = StatType.ONLINE_TIME; // Default sorting
        int limit = DEFAULT_LIMIT;
        
        if (args.length >= 1) {
            // Parse stat type
            try {
                statType = parseStatType(args[0]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(new TextComponent(configManager.getMessage("top.invalid_stat_type", "type", args[0])));
                sender.sendMessage(new TextComponent(configManager.getMessage("top.usage")));
                return;
            }
            
            // Parse limit if provided
            if (args.length >= 2) {
                try {
                    limit = Integer.parseInt(args[1]);
                    if (limit <= 0) {
                        limit = DEFAULT_LIMIT;
                    } else if (limit > 50) {
                        limit = 50; // Cap at 50 to prevent performance issues
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponent(configManager.getMessage("top.invalid_limit", "limit", args[1])));
                    limit = DEFAULT_LIMIT;
                }
            }
        }
        
        // Get and display top players
        final StatType finalStatType = statType;
        final int finalLimit = limit;
        
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            displayTopPlayers(sender, finalStatType, finalLimit);
        });
    }
    
    /**
     * Parse the stat type from the input string
     * 
     * @param input The input string
     * @return The corresponding StatType
     * @throws IllegalArgumentException If the input is not a valid stat type
     */
    private StatType parseStatType(String input) throws IllegalArgumentException {
        switch (input.toLowerCase()) {
            case "time":
            case "onlinetime":
            case "online":
                return StatType.ONLINE_TIME;
                
            case "logins":
            case "joins":
                return StatType.LOGIN_COUNT;
                
            case "votes":
            case "vote":
                return StatType.VOTES;
                
            default:
                throw new IllegalArgumentException("Invalid stat type: " + input);
        }
    }
    
    /**
     * Display the top players for a specific stat type
     * 
     * @param sender The command sender
     * @param statType The type of statistic to display
     * @param limit The maximum number of players to display
     */
    private void displayTopPlayers(CommandSender sender, StatType statType, int limit) {
        StatsManager statsManager = plugin.getStatsManager();
        List<PlayerStats> topPlayers = statsManager.getTopPlayers(statType, limit);
        
        if (topPlayers.isEmpty()) {
            sender.sendMessage(new TextComponent(configManager.getMessage("top.no_data")));
            return;
        }
        
        // Get stat type name for header
        String statTypeName;
        switch (statType) {
            case ONLINE_TIME:
                statTypeName = configManager.getMessage("top.stat_type.online_time");
                break;
            case LOGIN_COUNT:
                statTypeName = configManager.getMessage("top.stat_type.login_count");
                break;
            case VOTES:
                statTypeName = configManager.getMessage("top.stat_type.votes");
                break;
            default:
                statTypeName = statType.name();
        }
        
        // Send header
        sender.sendMessage(new TextComponent(configManager.getMessage("top.header", "stat_type", statTypeName, "limit", String.valueOf(limit))));
        
        // Send player rankings
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerStats stats = topPlayers.get(i);
            String value;
            
            // Format the value based on stat type
            switch (statType) {
                case ONLINE_TIME:
                    value = StatsManager.formatTimeDuration(stats.getTotalOnlineTime());
                    break;
                case LOGIN_COUNT:
                    value = String.valueOf(stats.getLoginCount());
                    break;
                case VOTES:
                    value = String.valueOf(stats.getVotes());
                    break;
                default:
                    value = "?";
            }
            
            // Send player ranking message
            sender.sendMessage(new TextComponent(configManager.getMessage("top.entry", 
                    "rank", String.valueOf(i + 1),
                    "player", stats.getName(),
                    "value", value)));
        }
        
        // Send footer
        sender.sendMessage(new TextComponent(configManager.getMessage("top.footer")));
    }
} 