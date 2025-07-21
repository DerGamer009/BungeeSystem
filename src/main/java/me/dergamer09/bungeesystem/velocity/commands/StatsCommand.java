package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.Managers.StatsManager;
import me.dergamer09.bungeesystem.velocity.Managers.StatsManager.PlayerStats;
import net.kyori.adventure.text.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class StatsCommand implements SimpleCommand {
    private final VelocitySystem plugin;
    private final ConfigManager configManager;
    private final StatsManager statsManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public StatsCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getMessageComponent("stats.console_must_specify_player"));
                return;
            }
            target = (Player) sender;
            displayPlayerStats(sender, target.getUniqueId(), target.getUsername());
            return;
        }

        String playerName = args[0];
        target = plugin.getServer().getPlayer(playerName).orElse(null);
        if (target != null) {
            displayPlayerStats(sender, target.getUniqueId(), target.getUsername());
        } else {
            lookupOfflinePlayer(sender, playerName);
        }
    }

    private void lookupOfflinePlayer(CommandSource sender, String playerName) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            UUID uuid = plugin.getDatabaseManager().getUUIDFromName(playerName);
            if (uuid == null) {
                sender.sendMessage(configManager.getMessageComponent("stats.player_not_found", "player", playerName));
                return;
            }
            displayPlayerStats(sender, uuid, playerName);
        }).schedule();
    }

    private void displayPlayerStats(CommandSource sender, UUID uuid, String playerName) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            PlayerStats stats = statsManager.getPlayerStats(uuid);
            if (stats == null) {
                sender.sendMessage(configManager.getMessageComponent("stats.no_stats_found", "player", playerName));
                return;
            }
            sender.sendMessage(configManager.getMessageComponent("stats.header", "player", playerName));
            if (stats.getFirstJoin() > 0) {
                String date = dateFormat.format(new Date(stats.getFirstJoin()));
                sender.sendMessage(configManager.getMessageComponent("stats.first_joined", "date", date));
            }
            if (plugin.getServer().getPlayer(uuid).isEmpty() && stats.getLastJoin() > 0) {
                String date = dateFormat.format(new Date(stats.getLastJoin()));
                sender.sendMessage(configManager.getMessageComponent("stats.last_seen", "date", date));
            }
            String onlineTime = StatsManager.formatTimeDuration(stats.getTotalOnlineTime());
            sender.sendMessage(configManager.getMessageComponent("stats.online_time", "time", onlineTime));
            sender.sendMessage(configManager.getMessageComponent("stats.login_count", "count", String.valueOf(stats.getLoginCount())));
            sender.sendMessage(configManager.getMessageComponent("stats.votes", "count", String.valueOf(stats.getVotes())));
            sender.sendMessage(configManager.getMessageComponent("stats.messages", "count", String.valueOf(stats.getMessagesSent())));
            sender.sendMessage(configManager.getMessageComponent("stats.footer"));
        }).schedule();
    }
}
