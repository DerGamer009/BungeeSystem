package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.PunishmentManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KickCommand extends Command implements TabExecutor {

    private final BungeeSystem plugin;
    private final PunishmentManager punishmentManager;

    public KickCommand(BungeeSystem plugin) {
        super("kick", "bungeesystem.command.kick");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
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
        
        // Check if the reason is an ID
        int reasonId;
        
        try {
            reasonId = Integer.parseInt(reasonInput);
        } catch (NumberFormatException e) {
            // Not an ID, try to find the reason by name
            reasonId = punishmentManager.getReasonId("KICK", reasonInput);
            
            if (reasonId == -1) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                        "Unknown kick reason: " + reasonInput));
                listAvailableReasons(sender);
                return;
            }
        }
        
        // Execute the kick
        boolean success = punishmentManager.kickPlayer(sender, targetName, reasonId, customReason);
        
        if (success) {
            // Log admin action to API
            if (plugin.getApiManager().isApiEnabled()) {
                String adminName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
                String reasonName = punishmentManager.getReasonName(reasonId);
                String details = customReason != null && !customReason.isEmpty() ? 
                        reasonName + ": " + customReason : reasonName;
                
                plugin.getApiManager().sendAdminLog(adminName, "kick", targetName, details);
            }
        } else {
            // The punishmentManager will send appropriate error messages
        }
    }
    
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                "Usage: /kick <player> <reason/id> [custom text]"));
        listAvailableReasons(sender);
    }
    
    private void listAvailableReasons(CommandSender sender) {
        List<Map<String, Object>> reasons = punishmentManager.getReasons("KICK");
        
        sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                "Available kick reasons:"));
        
        for (Map<String, Object> reason : reasons) {
            int id = (int)reason.get("id");
            String name = (String)reason.get("name");
            String description = (String)reason.get("description");
            
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                    "  " + id + " - " + name + ": " + description));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest online players
            suggestions.addAll(plugin.getProxy().getPlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            // Suggest kick reasons
            List<Map<String, Object>> reasons = punishmentManager.getReasons("KICK");
            for (Map<String, Object> reason : reasons) {
                String name = (String)reason.get("name");
                if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add(name);
                }
            }
        }
        
        return suggestions;
    }
} 