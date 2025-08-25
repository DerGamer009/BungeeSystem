package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.Managers.PunishmentManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ban command implementation for Velocity
 */
public class BanCommand implements SimpleCommand {

    private final VelocitySystem plugin;
    private final PunishmentManager punishmentManager;
    private final ConfigManager configManager;

    public BanCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments().toArray(new String[0]);
        
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
        long duration = -1; // Default to permanent
        
        try {
            reasonId = Integer.parseInt(reasonInput);
        } catch (NumberFormatException e) {
            // Not an ID, try to find the reason by name
            reasonId = punishmentManager.getReasonId("BAN", reasonInput);
            
            if (reasonId == -1) {
                String message = configManager.getMessage("punishment.unknown_reason", 
                        "type", "ban", "reason", reasonInput);
                sender.sendMessage(Component.text(configManager.getPrefix() + message));
                listAvailableReasons(sender);
                return;
            }
        }
        
        // Get the ban reason's duration
        List<Map<String, Object>> banReasons = punishmentManager.getReasons("BAN");
        for (Map<String, Object> reason : banReasons) {
            if ((int)reason.get("id") == reasonId) {
                duration = (long)reason.get("duration");
                break;
            }
        }
        
        // Execute the ban
        boolean success = punishmentManager.banPlayer(sender, targetName, reasonId, customReason, duration);
        
        if (!success) {
            // The punishmentManager will send appropriate error messages
        }
    }
    
    private void sendUsage(CommandSource sender) {
        String message = configManager.getMessage("punishment.ban.usage");
        sender.sendMessage(Component.text(configManager.getPrefix() + message));
        listAvailableReasons(sender);
    }
    
    private void listAvailableReasons(CommandSource sender) {
        List<Map<String, Object>> reasons = punishmentManager.getReasons("BAN");
        
        String message = configManager.getMessage("punishment.available_reasons", "type", "ban");
        sender.sendMessage(Component.text(configManager.getPrefix() + message));
        
        for (Map<String, Object> reason : reasons) {
            int id = (int)reason.get("id");
            String name = (String)reason.get("name");
            String description = (String)reason.get("description");
            long duration = (long)reason.get("duration");
            
            String durationStr = duration < 0 ? "Permanent" : 
                formatDuration(duration);
            
            Component component = Component.text(configManager.getDefaultMessageColor() + "  #" + id + ": " + 
                    name + " (" + durationStr + ")")
                    .hoverEvent(HoverEvent.showText(Component.text("§7" + description + "\n§eClick to use this reason")))
                    .clickEvent(ClickEvent.suggestCommand("/ban " + name));
            
            sender.sendMessage(component);
        }
    }
    
    /**
     * Format duration in seconds to human readable format
     */
    private String formatDuration(long seconds) {
        if (seconds < 0) return "Permanent";
        
        if (seconds < 60) return seconds + " seconds";
        if (seconds < 3600) return (seconds / 60) + " minutes";
        if (seconds < 86400) return (seconds / 3600) + " hours";
        return (seconds / 86400) + " days";
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        List<String> suggestions = new ArrayList<>();
        String[] args = invocation.arguments().toArray(new String[0]);
        
        if (args.length == 1) {
            // Suggest online player names
            String query = args[0].toLowerCase();
            plugin.getServer().getAllPlayers().forEach(player -> {
                if (player.getUsername().toLowerCase().startsWith(query)) {
                    suggestions.add(player.getUsername());
                }
            });
        } else if (args.length == 2) {
            // Suggest ban reasons
            String query = args[1].toLowerCase();
            List<Map<String, Object>> reasons = punishmentManager.getReasons("BAN");
            for (Map<String, Object> reason : reasons) {
                String name = (String) reason.get("name");
                String id = String.valueOf(reason.get("id"));
                
                if (name.toLowerCase().startsWith(query) || id.startsWith(query)) {
                    suggestions.add(name);
                    suggestions.add(id);
                }
            }
        }
        
        return suggestions;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bungeesystem.command.ban");
    }
}
