package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import net.kyori.adventure.text.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SeenCommand implements SimpleCommand {
    private final VelocitySystem plugin;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    public SeenCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length < 1) {
            source.sendMessage(configManager.getMessageComponent("seen.usage"));
            return;
        }
        String playerName = args[0];
        Optional<Player> targetOpt = plugin.getServer().getPlayer(playerName);
        if (targetOpt.isPresent() && targetOpt.get().isActive()) {
            // Update last_seen für den Spieler
            plugin.getDatabaseManager().updateLastSeen(targetOpt.get().getUniqueId(), System.currentTimeMillis());
            source.sendMessage(configManager.getMessageComponent("seen.currently_online", "player", targetOpt.get().getUsername()));
            return;
        }
        // Async DB lookup
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            try {
                UUID uuid = getUUIDFromName(playerName);
                if (uuid == null) {
                    source.sendMessage(configManager.getMessageComponent("seen.player_never_joined", "player", playerName));
                    return;
                }
                long lastOnline = getLastOnlineTime(uuid);
                if (lastOnline == 0) {
                    source.sendMessage(configManager.getMessageComponent("seen.no_last_seen_data", "player", playerName));
                    return;
                }
                String formattedDate = dateFormat.format(new Date(lastOnline));
                long timeDiff = System.currentTimeMillis() - lastOnline;
                String timeAgo = formatTimeDifference(timeDiff);
                source.sendMessage(configManager.getMessageComponent("seen.last_seen", "player", playerName, "date", formattedDate, "time_ago", timeAgo));
            } catch (Exception e) {
                source.sendMessage(configManager.getMessageComponent("seen.database_error", "error", e.getMessage()));
                plugin.getLogger().error("Error in SeenCommand: {}", e.getMessage());
            }
        }).schedule();
    }
    private UUID getUUIDFromName(String playerName) throws Exception {
        Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM player_data WHERE name = ? LIMIT 1");
        ps.setString(1, playerName);
        ResultSet rs = ps.executeQuery();
        UUID uuid = null;
        if (rs.next()) uuid = UUID.fromString(rs.getString("uuid"));
        rs.close();
        ps.close();
        return uuid;
    }
    private long getLastOnlineTime(UUID uuid) throws Exception {
        Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT last_seen FROM player_data WHERE uuid = ? LIMIT 1");
        ps.setString(1, uuid.toString());
        ResultSet rs = ps.executeQuery();
        long lastSeen = 0;
        if (rs.next()) lastSeen = rs.getLong("last_seen");
        rs.close();
        ps.close();
        return lastSeen;
    }
    private String formatTimeDifference(long timeDiffMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiffMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiffMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(timeDiffMillis) % 24;
        long days = TimeUnit.MILLISECONDS.toDays(timeDiffMillis);
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day" : " days");
        if (hours > 0) { if (sb.length() > 0) sb.append(", "); sb.append(hours).append(hours == 1 ? " hour" : " hours"); }
        if (minutes > 0 && days == 0) { if (sb.length() > 0) sb.append(", "); sb.append(minutes).append(minutes == 1 ? " minute" : " minutes"); }
        if (seconds > 0 && hours == 0 && days == 0) { if (sb.length() > 0) sb.append(", "); sb.append(seconds).append(seconds == 1 ? " second" : " seconds"); }
        if (sb.length() == 0) sb.append("just now"); else sb.append(" ago");
        return sb.toString();
    }
}
