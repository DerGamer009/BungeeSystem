package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.util.TokenGenerator;

/**
 * Command to generate server tokens for API authentication
 */
public class GenerateTokenCommand implements SimpleCommand {
    
    private final VelocitySystem plugin;
    
    public GenerateTokenCommand(VelocitySystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        // Check permission
        if (source instanceof Player) {
            Player player = (Player) source;
            if (!player.hasPermission("bungeesystem.generatetoken")) {
                source.sendMessage(Component.text("You don't have permission to use this command!")
                        .color(NamedTextColor.RED));
                return;
            }
        }
        
        boolean force = args.length > 0 && args[0].equalsIgnoreCase("force");
        
        try {
            String token = TokenGenerator.generateAndSaveToken(plugin);
            
            if (token != null) {
                source.sendMessage(Component.text("✅ Server token generated successfully!")
                        .color(NamedTextColor.GREEN));
                source.sendMessage(Component.text("Token: " + maskToken(token))
                        .color(NamedTextColor.YELLOW));
                source.sendMessage(Component.text("💡 Full token saved to token.yml")
                        .color(NamedTextColor.GRAY));
                
                // Reload API manager with new token
                plugin.getApiManager().reloadToken();
                
            } else {
                source.sendMessage(Component.text("❌ Failed to generate token!")
                        .color(NamedTextColor.RED));
            }
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ Error generating token: " + e.getMessage())
                    .color(NamedTextColor.RED));
            plugin.getLogger().error("Error in generatetoken command", e);
        }
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player) {
            return ((Player) source).hasPermission("bungeesystem.generatetoken");
        }
        return true; // Console always has permission
    }
    
    /**
     * Mask token for display
     */
    private String maskToken(String token) {
        if (token.length() <= 8) return "***";
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}