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
 * Warn command implementation for Velocity
 */
public class WarnCommand implements SimpleCommand {

    private final VelocitySystem plugin;
    private final PunishmentManager punishmentManager;
    private final ConfigManager configManager;

    public WarnCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        
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
            reasonId = punishmentManager.getReasonId("WARN", reasonInput);
            
            if (reasonId == -1) {
                String message = configManager.getMessage("punishment.unknown_reason", 
                        "type", "warn", "reason", reasonInput);
                sender.sendMessage(Component.text(configManager.getPrefix() + message));
                listAvailableReasons(sender);
                return;
            }
        }
        
        // Execute the warning
        boolean success = punishmentManager.warnPlayer(sender, targetName, reasonId, customReason);
        
        if (!success) {
            // The punishmentManager will send appropriate error messages
        }
    }
    
    private void sendUsage(CommandSource sender) {
        String message = configManager.getMessage("punishment.warn.usage");
        sender.sendMessage(Component.text(configManager.getPrefix() + message));
        listAvailableReasons(sender);
    }
    
    private void listAvailableReasons(CommandSource sender) {
        List<Map<String, Object>> reasons = punishmentManager.getReasons("WARN");
        
        String message = configManager.getMessage("punishment.available_reasons", "type", "warn");
        sender.sendMessage(Component.text(configManager.getPrefix() + message));
        
        for (Map<String, Object> reason : reasons) {
            int id = (int)reason.get("id");
            String name = (String)reason.get("name");
            String description = (String)reason.get("description");
            
            Component component = Component.text(configManager.getDefaultMessageColor() + "  #" + id + ": " + name)
                    .hoverEvent(HoverEvent.showText(Component.text("§7" + description + "\n§eClick to use this reason")))
                    .clickEvent(ClickEvent.suggestCommand("/warn " + name));
            
            sender.sendMessage(component);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        List<String> suggestions = new ArrayList<>();
        String[] args = invocation.arguments();
        
        if (args.length == 1) {
            // Suggest online player names
            String query = args[0].toLowerCase();
            plugin.getServer().getAllPlayers().forEach(player -> {
                if (player.getUsername().toLowerCase().startsWith(query)) {
                    suggestions.add(player.getUsername());
                }
            });
        } else if (args.length == 2) {
            // Suggest warn reasons
            String query = args[1].toLowerCase();
            List<Map<String, Object>> reasons = punishmentManager.getReasons("WARN");
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
        return invocation.source().hasPermission("bungeesystem.command.warn");
    }
}
