package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

/**
 * Command to validate the server token with the backend
 */
public class ValidateTokenCommand extends Command {

    private final BungeeSystem plugin;

    public ValidateTokenCommand() {
        super("validatetoken", "bungeesystem.admin.validatetoken", "validatetoken", "checktoken");
        this.plugin = BungeeSystem.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("bungeesystem.admin.validatetoken")) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command!"));
            return;
        }

        // Check if API is enabled
        if (!plugin.getApiManager().isApiEnabled()) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "❌ API is disabled or no token configured!"));
            return;
        }

        // Start validation
        sender.sendMessage(new TextComponent(ChatColor.YELLOW + "🔍 Validating server token with backend..."));
        
        // Run validation asynchronously
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            boolean isValid = plugin.getApiManager().validateServerToken();
            
            // Send result back to sender
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                if (isValid) {
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "✅ Server token validation successful!"));
                    sender.sendMessage(new TextComponent(ChatColor.AQUA + "Your server is now connected to the dashboard."));
                } else {
                    sender.sendMessage(new TextComponent(ChatColor.RED + "❌ Server token validation failed!"));
                    sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Check your token configuration and try again."));
                }
            });
        });
    }
} 