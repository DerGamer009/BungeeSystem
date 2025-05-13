package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.PunishmentManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportsCommand extends Command implements TabExecutor {

    private final BungeeSystem plugin;
    private final PunishmentManager punishmentManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public ReportsCommand(BungeeSystem plugin) {
        super("reports", "bungeesystem.command.reports", "reportlist");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Show list of active reports
            listReports(sender);
            return;
        }

        String action = args[0].toLowerCase();
        
        if (action.equals("help")) {
            sendHelp(sender);
            return;
        }
        
        if (action.equals("handle") || action.equals("close")) {
            if (args.length < 2) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                        "Usage: /reports handle <id> [comment]"));
                return;
            }
            
            int reportId;
            try {
                reportId = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                        "Invalid report ID. Please provide a valid number."));
                return;
            }
            
            String comment = "Handled by staff";
            if (args.length > 2) {
                StringBuilder commentBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    commentBuilder.append(args[i]).append(" ");
                }
                comment = commentBuilder.toString().trim();
            }
            
            // Handle the report
            boolean success = punishmentManager.handleReport(sender, reportId, comment);
            
            if (!success) {
                // The punishmentManager will send appropriate error messages
            }
            
            return;
        }
        
        // Unknown action
        sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                "Unknown action: " + action));
        sendHelp(sender);
    }
    
    private void listReports(CommandSender sender) {
        List<Map<String, Object>> reports = punishmentManager.getActiveReports(10);
        
        if (reports.isEmpty()) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                    "There are no active reports."));
            return;
        }
        
        sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                "Active reports (showing up to 10):"));
        
        for (Map<String, Object> report : reports) {
            int id = (int)report.get("id");
            String playerName = (String)report.get("player_name");
            String reporterName = (String)report.get("reporter_name");
            String reasonName = (String)report.get("reason_name");
            String customReason = (String)report.get("custom_reason");
            String server = (String)report.get("server");
            long timestamp = (long)report.get("timestamp");
            
            // Format the reason
            String reason = reasonName;
            if (customReason != null && !customReason.isEmpty()) {
                reason += ": " + customReason;
            }
            
            // Format the timestamp
            String timeStr = dateFormat.format(new Date(timestamp));
            
            // Build the component with hover and click events
            TextComponent component = new TextComponent(plugin.getDefaultMessageColor() + "#" + id + ": " + 
                    ChatColor.RED + playerName + plugin.getDefaultMessageColor() + " reported by " + 
                    ChatColor.YELLOW + reporterName + plugin.getDefaultMessageColor() + " on " + server);
            
            // Add hover with details
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                    ChatColor.GOLD + "Report #" + id + "\n" +
                    ChatColor.WHITE + "Player: " + ChatColor.RED + playerName + "\n" +
                    ChatColor.WHITE + "Reporter: " + ChatColor.YELLOW + reporterName + "\n" +
                    ChatColor.WHITE + "Server: " + server + "\n" +
                    ChatColor.WHITE + "Time: " + timeStr + "\n" +
                    ChatColor.WHITE + "Reason: " + reason + "\n\n" +
                    ChatColor.GREEN + "Click to handle this report"
            )));
            
            // Add click event to suggest the handle command
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                    "/reports handle " + id + " "));
            
            sender.sendMessage(component);
        }
        
        // Add footer with instructions
        sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + 
                "Use " + ChatColor.YELLOW + "/reports handle <id> [comment]" + plugin.getDefaultMessageColor() + 
                " to handle a report."));
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                "Reports Command Help:"));
        
        TextComponent listCmd = new TextComponent(ChatColor.YELLOW + "/reports" + 
                plugin.getDefaultMessageColor() + " - List active reports");
        listCmd.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports"));
        
        TextComponent handleCmd = new TextComponent(ChatColor.YELLOW + "/reports handle <id> [comment]" + 
                plugin.getDefaultMessageColor() + " - Handle/close a report");
        handleCmd.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/reports handle "));
        
        sender.sendMessage(listCmd);
        sender.sendMessage(handleCmd);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String search = args[0].toLowerCase();
            List<String> actions = new ArrayList<>();
            actions.add("help");
            actions.add("handle");
            actions.add("close");
            
            return actions.stream()
                    .filter(action -> action.startsWith(search))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("handle") || args[0].equalsIgnoreCase("close"))) {
            // Tab complete report IDs
            String search = args[1].toLowerCase();
            
            // Get active report IDs
            List<String> reportIds = punishmentManager.getActiveReports(20).stream()
                    .map(report -> String.valueOf(report.get("id")))
                    .filter(id -> id.startsWith(search))
                    .collect(Collectors.toList());
            
            return reportIds;
        }
        
        return new ArrayList<>();
    }
} 