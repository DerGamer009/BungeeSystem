package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

public class ReportCommand implements SimpleCommand {
    private final VelocitySystem plugin;
    private final ConfigManager configManager;
    public ReportCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source instanceof Player)) {
            source.sendMessage(configManager.getMessageComponent("lobby.player_only"));
            return;
        }
        Player player = (Player) source;
        if (args.length < 2) {
            player.sendMessage(configManager.getMessageComponent("report.usage"));
            // TODO: List available reasons
            return;
        }
        String targetName = args[0];
        String reason = args[1];
        String customReason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
        if (targetName.equalsIgnoreCase(player.getUsername())) {
            player.sendMessage(configManager.getMessageComponent("report.cannot_report_self"));
            return;
        }
        Optional<Player> targetOpt = plugin.getServer().getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            player.sendMessage(configManager.getMessageComponent("report.player_not_found"));
            return;
        }
        // Grund-Logik: ID oder Name
        int reasonId = -1;
        List<Map<String, Object>> reasons = plugin.getDatabaseManager().getReasons("REPORT");
        try {
            reasonId = Integer.parseInt(reason);
        } catch (NumberFormatException e) {
            for (Map<String, Object> r : reasons) {
                if (r.get("name").toString().equalsIgnoreCase(reason)) {
                    reasonId = (int) r.get("id");
                    break;
                }
            }
        }
        if (reasonId == -1) {
            player.sendMessage(configManager.getMessageComponent("report.unknown_reason", "reason", reason));
            // Gründe schön formatiert auflisten
            player.sendMessage(Component.text("§eAvailable report reasons:"));
            for (Map<String, Object> r : reasons) {
                String id = String.valueOf(r.get("id"));
                String name = r.get("name").toString();
                String desc = r.get("description") != null ? r.get("description").toString() : "";
                player.sendMessage(Component.text("§7#" + id + ": §a" + name + (desc.isEmpty() ? "" : " §8- §f" + desc)));
            }
            return;
        }
        // Report speichern
        UUID targetUUID = plugin.getDatabaseManager().getUUIDFromName(targetName);
        if (targetUUID == null) {
            player.sendMessage(configManager.getMessageComponent("report.player_not_found"));
            return;
        }
        boolean success = plugin.getDatabaseManager().insertReport(
            targetUUID, targetName, player.getUniqueId(), player.getUsername(), reasonId, customReason, player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "", System.currentTimeMillis()
        );
        String reasonName = "";
        for (Map<String, Object> r : reasons) {
            if (((int) r.get("id")) == reasonId) {
                reasonName = r.get("name").toString();
                break;
            }
        }
        if (success) {
            player.sendMessage(configManager.getMessageComponent("report.success", "player", targetName, "reason", reasonName));
            // Discord Webhook senden, falls gesetzt
            String webhookUrl = configManager.getConfig().getString("webhookUrl", "");
            if (!webhookUrl.isEmpty()) {
                String serverName = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "";
                String json = "{" +
                        "\"embeds\":[{" +
                        "\"title\":\"New Report\"," +
                        "\"color\":16711680," +
                        "\"fields\":[" +
                        "{\"name\":\"Player\",\"value\":\"" + targetName + "\",\"inline\":true}," +
                        "{\"name\":\"Reporter\",\"value\":\"" + player.getUsername() + "\",\"inline\":true}," +
                        "{\"name\":\"Reason\",\"value\":\"" + reasonName.replace("\"", "'") + (customReason != null && !customReason.isEmpty() ? (" (" + customReason.replace("\"", "'") + ")") : "") + "\",\"inline\":false}," +
                        "{\"name\":\"Server\",\"value\":\"" + serverName + "\",\"inline\":true}," +
                        "{\"name\":\"Time\",\"value\":\"" + Instant.now().toString() + "\",\"inline\":true}" +
                        "]}]}";
                HttpClient.newHttpClient().sendAsync(
                        HttpRequest.newBuilder()
                                .uri(URI.create(webhookUrl))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build(),
                        HttpResponse.BodyHandlers.discarding()
                ).exceptionally(ex -> {
                    plugin.getLogger().error("Failed to send Discord webhook: {}", ex.getMessage());
                    return null;
                });
            }
        } else {
            player.sendMessage(configManager.getMessageComponent("report.failed"));
        }
    }
    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String search = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : plugin.getServer().getAllPlayers()) {
                if (!p.getUsername().equalsIgnoreCase(invocation.source().toString()) && p.getUsername().toLowerCase().startsWith(search)) {
                    names.add(p.getUsername());
                }
            }
            return names;
        } else if (args.length == 2) {
            String search = args[1].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            for (Map<String, Object> r : plugin.getDatabaseManager().getReasons("REPORT")) {
                String id = String.valueOf(r.get("id"));
                String name = r.get("name").toString();
                if (id.startsWith(search)) suggestions.add(id);
                if (name.toLowerCase().startsWith(search)) suggestions.add(name);
            }
            return suggestions;
        }
        return Collections.emptyList();
    }
}
