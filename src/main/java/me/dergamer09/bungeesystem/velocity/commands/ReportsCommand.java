package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReportsCommand implements SimpleCommand {
    private final VelocitySystem plugin;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public ReportsCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length == 0) {
            // Zeige Liste aktiver Reports
            List<Map<String, Object>> reports = plugin.getDatabaseManager().getActiveReports(10);
            if (reports.isEmpty()) {
                source.sendMessage(configManager.getMessageComponent("reports.none"));
                return;
            }
            source.sendMessage(configManager.getMessageComponent("reports.header"));
            for (Map<String, Object> report : reports) {
                int id = (int) report.get("id");
                String playerName = (String) report.get("reported_name");
                String reporterName = (String) report.get("reported_by_name");
                String reasonName = (String) report.get("reason_name");
                String customReason = (String) report.get("reason");
                String server = (String) report.get("server");
                long timestamp = (long) report.get("timestamp");
                String reason = reasonName;
                if (customReason != null && !customReason.isEmpty()) {
                    reason += ": " + customReason;
                }
                String timeStr = dateFormat.format(new Date(timestamp));
                TextComponent msg = Component.text()
                        .append(Component.text("#" + id + ": ", NamedTextColor.GRAY))
                        .append(Component.text(playerName, NamedTextColor.RED))
                        .append(Component.text(" reported by ", NamedTextColor.GRAY))
                        .append(Component.text(reporterName, NamedTextColor.YELLOW))
                        .append(Component.text(" on ", NamedTextColor.GRAY))
                        .append(Component.text(server, NamedTextColor.AQUA))
                        .append(Component.text(" | Reason: ", NamedTextColor.GRAY))
                        .append(Component.text(reason, NamedTextColor.GOLD))
                        .append(Component.text(" | Time: ", NamedTextColor.GRAY))
                        .append(Component.text(timeStr, NamedTextColor.GREEN))
                        .build();
                source.sendMessage(msg);
            }
            source.sendMessage(configManager.getMessageComponent("reports.footer"));
            return;
        }
        String action = args[0].toLowerCase();
        if (action.equals("help")) {
            source.sendMessage(configManager.getMessageComponent("reports.help"));
            return;
        }
        if (action.equals("handle") || action.equals("close")) {
            if (args.length < 2) {
                source.sendMessage(configManager.getMessageComponent("reports.handle_usage"));
                return;
            }
            int reportId;
            try {
                reportId = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                source.sendMessage(configManager.getMessageComponent("reports.invalid_id"));
                return;
            }
            String comment = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Handled by staff";
            UUID staffUUID = null;
            String staffName = "Console";
            if (source instanceof com.velocitypowered.api.proxy.Player) {
                com.velocitypowered.api.proxy.Player p = (com.velocitypowered.api.proxy.Player) source;
                staffUUID = p.getUniqueId();
                staffName = p.getUsername();
            }
            boolean success = plugin.getDatabaseManager().handleReport(reportId, staffUUID != null ? staffUUID : UUID.randomUUID(), staffName, comment);
            if (success) {
                source.sendMessage(configManager.getMessageComponent("reports.handle_success", "id", String.valueOf(reportId)));
            } else {
                source.sendMessage(configManager.getMessageComponent("reports.handle_failed", "id", String.valueOf(reportId)));
            }
            return;
        }
        source.sendMessage(configManager.getMessageComponent("reports.not_implemented"));
    }
    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String search = args[0].toLowerCase();
            List<String> actions = Arrays.asList("help", "handle", "close");
            List<String> result = new ArrayList<>();
            for (String a : actions) if (a.startsWith(search)) result.add(a);
            return result;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("handle") || args[0].equalsIgnoreCase("close"))) {
            String search = args[1].toLowerCase();
            List<String> ids = new ArrayList<>();
            for (Map<String, Object> r : plugin.getDatabaseManager().getActiveReports(20)) {
                String id = String.valueOf(r.get("id"));
                if (id.startsWith(search)) ids.add(id);
            }
            return ids;
        }
        return Collections.emptyList();
    }
}
