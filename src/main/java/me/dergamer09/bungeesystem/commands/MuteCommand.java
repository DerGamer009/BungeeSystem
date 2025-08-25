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

public class MuteCommand extends Command implements TabExecutor {

    private final BungeeSystem plugin;
    private final PunishmentManager punishmentManager;

    public MuteCommand(BungeeSystem plugin) {
        super("mute", "bungeesystem.command.mute", "gmute", "tempmute");
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
        long duration = -1; // Default to permanent
        
        try {
            reasonId = Integer.parseInt(reasonInput);
        } catch (NumberFormatException e) {
            // Not an ID, try to find the reason by name
            reasonId = punishmentManager.getReasonId("MUTE", reasonInput);
            
            if (reasonId == -1) {
                String message = plugin.getConfigManager().getMessage("punishment.unknown_reason", 
                        "type", "mute", "reason", reasonInput);
                sender.sendMessage(new TextComponent(plugin.getPrefix() + message));
                listAvailableReasons(sender);
                return;
            }
        }
        
        // Get the mute reason's duration
        List<Map<String, Object>> muteReasons = punishmentManager.getReasons("MUTE");
        for (Map<String, Object> reason : muteReasons) {
            if ((int)reason.get("id") == reasonId) {
                duration = (long)reason.get("duration");
                break;
            }
        }
        
        // Execute the mute
        boolean success = punishmentManager.mutePlayer(sender, targetName, reasonId, customReason, duration);
        
        if (!success) {
            // The punishmentManager will send appropriate error messages
        }
    }
    
    private void sendUsage(CommandSender sender) {
        String message = plugin.getConfigManager().getMessage("punishment.mute.usage");
        sender.sendMessage(new TextComponent(plugin.getPrefix() + message));
        listAvailableReasons(sender);
    }
    
    private void listAvailableReasons(CommandSender sender) {
        List<Map<String, Object>> reasons = punishmentManager.getReasons("MUTE");
        
        String message = plugin.getConfigManager().getMessage("punishment.available_reasons", "type", "mute");
        sender.sendMessage(new TextComponent(plugin.getPrefix() + message));
        
        for (Map<String, Object> reason : reasons) {
            int id = (int)reason.get("id");
            String name = (String)reason.get("name");
            String description = (String)reason.get("description");
            long duration = (long)reason.get("duration");
            
            String durationStr = duration < 0 ? "Permanent" : 
                formatDuration(duration);
            
            TextComponent component = new TextComponent(plugin.getDefaultMessageColor() + "  #" + id + ": " + 
                    name + " (" + durationStr + ")");
            
            // Add hover text with description
            if (description != null && !description.isEmpty()) {
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new Text(description + "\nClick to use this reason")));
            }
            
            // Add click event to auto-fill command
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                    "/mute " + (sender instanceof ProxiedPlayer ? "" : "playerName ") + id));
            
            sender.sendMessage(component);
        }
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        
        if (months > 0) {
            return months + (months == 1 ? " month" : " months");
        } else if (days > 0) {
            return days + (days == 1 ? " day" : " days");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        } else {
            return seconds + (seconds == 1 ? " second" : " seconds");
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Tab complete player names
            String search = args[0].toLowerCase();
            return ProxyServer.getInstance().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(search))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Tab complete mute reasons
            String search = args[1].toLowerCase();
            List<String> reasons = new ArrayList<>();
            
            // Add reason IDs and names
            for (Map<String, Object> reason : punishmentManager.getReasons("MUTE")) {
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