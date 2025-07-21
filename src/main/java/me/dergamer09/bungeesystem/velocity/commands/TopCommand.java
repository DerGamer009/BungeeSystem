package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.command.CommandSource;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.Managers.StatsManager;
import me.dergamer09.bungeesystem.velocity.Managers.StatsManager.PlayerStats;
import me.dergamer09.bungeesystem.velocity.Managers.StatsManager.StatType;
import net.kyori.adventure.text.Component;

import java.util.List;

public class TopCommand implements SimpleCommand {
    private final VelocitySystem plugin;
    private final ConfigManager configManager;
    private final StatsManager statsManager;
    private final int DEFAULT_LIMIT = 10;

    public TopCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        StatType statType = StatType.ONLINE_TIME;
        int limit = DEFAULT_LIMIT;

        if (args.length >= 1) {
            try {
                statType = parseStatType(args[0]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(configManager.getMessageComponent("top.invalid_stat_type", "type", args[0]));
                sender.sendMessage(configManager.getMessageComponent("top.usage"));
                return;
            }
            if (args.length >= 2) {
                try {
                    limit = Integer.parseInt(args[1]);
                    if (limit <= 0) limit = DEFAULT_LIMIT;
                    else if (limit > 50) limit = 50;
                } catch (NumberFormatException e) {
                    sender.sendMessage(configManager.getMessageComponent("top.invalid_limit", "limit", args[1]));
                    limit = DEFAULT_LIMIT;
                }
            }
        }

        StatType finalStat = statType;
        int finalLimit = limit;
        plugin.getServer().getScheduler().buildTask(plugin, () -> displayTopPlayers(sender, finalStat, finalLimit)).schedule();
    }

    private StatType parseStatType(String input) {
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

    private void displayTopPlayers(CommandSource sender, StatType statType, int limit) {
        List<PlayerStats> topPlayers = statsManager.getTopPlayers(statType, limit);
        if (topPlayers.isEmpty()) {
            sender.sendMessage(configManager.getMessageComponent("top.no_data"));
            return;
        }
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
        sender.sendMessage(configManager.getMessageComponent("top.header", "stat_type", statTypeName, "limit", String.valueOf(limit)));
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerStats stats = topPlayers.get(i);
            String value;
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
            sender.sendMessage(configManager.getMessageComponent("top.entry", "rank", String.valueOf(i + 1), "player", stats.getName(), "value", value));
        }
        sender.sendMessage(configManager.getMessageComponent("top.footer"));
    }
}
