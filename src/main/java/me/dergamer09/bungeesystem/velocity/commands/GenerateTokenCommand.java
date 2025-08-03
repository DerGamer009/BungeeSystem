package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.util.TokenGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Command to generate a new server token for API access (Velocity version)
 */
public class GenerateTokenCommand implements SimpleCommand {

    private final VelocitySystem plugin;
    private final ProxyServer server;

    public GenerateTokenCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Check permission
        if (!source.hasPermission("bungeesystem.admin.generatetoken")) {
            source.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return;
        }

        // Check if token already exists
        if (TokenGenerator.tokenExists(plugin) && (args.length == 0 || !args[0].equalsIgnoreCase("force"))) {
            String currentToken = TokenGenerator.getCurrentToken(plugin);
            source.sendMessage(Component.text("A token already exists: " + 
                    TokenGenerator.maskToken(currentToken), NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Use /generatetoken force to generate a new one.", NamedTextColor.YELLOW));
            return;
        }

        // Generate new token
        source.sendMessage(Component.text("Generating new server token...", NamedTextColor.GREEN));
        
        String newToken = TokenGenerator.generateAndSaveToken(plugin);
        
        if (newToken != null) {
            source.sendMessage(Component.text("✅ New token generated successfully!", NamedTextColor.GREEN));
            source.sendMessage(Component.text("Token: " + newToken, NamedTextColor.AQUA));
            source.sendMessage(Component.text("Token has been saved to token.yml", NamedTextColor.GRAY));
            source.sendMessage(Component.text("⚠️  Keep this token secure and don't share it!", NamedTextColor.YELLOW));
            
            // Reload API Manager with new token
            plugin.getApiManager().reloadToken();
            
        } else {
            source.sendMessage(Component.text("❌ Failed to generate token. Check console for errors.", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return List.of("force");
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bungeesystem.admin.generatetoken");
    }
} 