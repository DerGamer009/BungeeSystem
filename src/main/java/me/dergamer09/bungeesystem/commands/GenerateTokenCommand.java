package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.util.TokenGenerator;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

/**
 * Command to generate a new server token for API access
 */
public class GenerateTokenCommand extends Command {

    private final BungeeSystem plugin;

    public GenerateTokenCommand() {
        super("generatetoken", "bungeesystem.admin.generatetoken", "gentoken", "newtoken");
        this.plugin = BungeeSystem.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("bungeesystem.admin.generatetoken")) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command!"));
            return;
        }

        // Check if token already exists
        if (TokenGenerator.tokenExists(plugin) && (args.length == 0 || !args[0].equalsIgnoreCase("force"))) {
            String currentToken = TokenGenerator.getCurrentToken(plugin);
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "A token already exists: " + 
                    TokenGenerator.maskToken(currentToken)));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Use /generatetoken force to generate a new one."));
            return;
        }

        // Generate new token
        sender.sendMessage(new TextComponent(ChatColor.GREEN + "Generating new server token..."));
        
        String newToken = TokenGenerator.generateAndSaveToken(plugin);
        
        if (newToken != null) {
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "✅ New token generated successfully!"));
            sender.sendMessage(new TextComponent(ChatColor.AQUA + "Token: " + ChatColor.WHITE + newToken));
            sender.sendMessage(new TextComponent(ChatColor.GRAY + "Token has been saved to token.yml"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "⚠️  Keep this token secure and don't share it!"));
            
            // Reload API Manager with new token
            plugin.getApiManager().reloadToken();
            
        } else {
            sender.sendMessage(new TextComponent(ChatColor.RED + "❌ Failed to generate token. Check console for errors."));
        }
    }
} 