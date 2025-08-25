package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.PunishmentManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportCommand extends Command implements TabExecutor {

    private final BungeeSystem plugin;
    private final PunishmentManager punishmentManager;

    public ReportCommand(BungeeSystem plugin) {
        super("report", "bungeesystem.command.report");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Only players can report
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                    "Only players can use the report command."));
            return;
        }
        
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        String targetName = args[0];
        String reasonInput = args[1];
        String customReason = null;
        
        if (args.length > 2) {
            StringBuilder customReasonBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                customReasonBuilder.append(args[i]).append(" ");
            }
            customReason = customReasonBuilder.toString().trim();
        }
        
        // Check if the player is trying to report themselves
        if (targetName.equalsIgnoreCase(sender.getName())) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                    "You cannot report yourself."));
            return;
        }
        
        // Check if the target player exists/is online
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
        if (target == null) {
            // Check if we can find the player in the database
            if (plugin.getDatabaseManager().getUUIDFromName(targetName) == null) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                        "Player not found. Make sure the name is spelled correctly."));
                return;
            }
        }
        
        // Check if the reason is an ID
        int reasonId;
        
        try {
            reasonId = Integer.parseInt(reasonInput);
        } catch (NumberFormatException e) {
            // Not an ID, try to find the reason by name
            reasonId = punishmentManager.getReasonId("REPORT", reasonInput);
            
            if (reasonId == -1) {
                String message = plugin.getConfigManager().getMessage("punishment.unknown_reason", 
                        "type", "report", "reason", reasonInput);
                sender.sendMessage(new TextComponent(plugin.getPrefix() + message));
                listAvailableReasons(sender);
                return;
            }
        }
        
        // Execute the report
        boolean success = punishmentManager.reportPlayer(sender, targetName, reasonId, customReason);
        
        if (!success) {
            // The punishmentManager will send appropriate error messages
        }
    }
    
    private void sendUsage(CommandSender sender) {
        String message = plugin.getConfigManager().getMessage("report.usage");
        sender.sendMessage(new TextComponent(plugin.getPrefix() + message));
        listAvailableReasons(sender);
    }
    
    private void listAvailableReasons(CommandSender sender) {
        List<Map<String, Object>> reasons = punishmentManager.getReasons("REPORT");
        
        String message = plugin.getConfigManager().getMessage("punishment.available_reasons", "type", "report");
        sender.sendMessage(new TextComponent(plugin.getPrefix() + message));
        
        for (Map<String, Object> reason : reasons) {
            int id = (int)reason.get("id");
            String name = (String)reason.get("name");
            String description = (String)reason.get("description");
            
            TextComponent component = new TextComponent(plugin.getDefaultMessageColor() + "  #" + id + ": " + name);
            
            // Add hover text with description
            if (description != null && !description.isEmpty()) {
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new Text(description + "\nClick to use this reason")));
            }
            
            // Add click event to auto-fill command
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                    "/report " + id));
            
            sender.sendMessage(component);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Tab complete player names (exclude self)
            String search = args[0].toLowerCase();
            return ProxyServer.getInstance().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .filter(name -> !name.equals(sender.getName())) // Exclude self
                    .filter(name -> name.toLowerCase().startsWith(search))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Tab complete report reasons
            String search = args[1].toLowerCase();
            List<String> reasons = new ArrayList<>();
            
            // Add reason IDs and names
            for (Map<String, Object> reason : punishmentManager.getReasons("REPORT")) {
                String id = String.valueOf(reason.get("id"));
                String name = (String)reason.get("name");
                
                if (id.startsWith(search)) {
                    reasons.add(id);
                }
                if (name.toLowerCase().startsWith(search)) {
                    reasons.add(name);
                }
            }
            
            return reasons;
        }
        
        return new ArrayList<>();
    }
} 