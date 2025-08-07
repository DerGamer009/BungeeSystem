package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;

import java.util.concurrent.CompletableFuture;

/**
 * Command to validate server token with the backend API
 */
public class ValidateTokenCommand implements SimpleCommand {
    
    private final VelocitySystem plugin;
    
    public ValidateTokenCommand(VelocitySystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        
        // Check permission
        if (source instanceof Player) {
            Player player = (Player) source;
            if (!player.hasPermission("bungeesystem.validatetoken")) {
                source.sendMessage(Component.text("You don't have permission to use this command!")
                        .color(NamedTextColor.RED));
                return;
            }
        }
        
        if (!plugin.getApiManager().isApiEnabled()) {
            source.sendMessage(Component.text("❌ API is disabled in config.yml")
                    .color(NamedTextColor.RED));
            return;
        }
        
        source.sendMessage(Component.text("🔄 Validating server token with backend...")
                .color(NamedTextColor.YELLOW));
        
        // Validate token asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                boolean isValid = plugin.getApiManager().validateServerToken();
                
                if (isValid) {
                    source.sendMessage(Component.text("✅ Server token validation successful!")
                            .color(NamedTextColor.GREEN));
                    source.sendMessage(Component.text("🎉 Your server is ready for dashboard integration!")
                            .color(NamedTextColor.GREEN));
                } else {
                    source.sendMessage(Component.text("❌ Server token validation failed!")
                            .color(NamedTextColor.RED));
                    source.sendMessage(Component.text("💡 Try generating a new token: /generatetoken force")
                            .color(NamedTextColor.GRAY));
                }
                
            } catch (Exception e) {
                source.sendMessage(Component.text("❌ Error validating token: " + e.getMessage())
                        .color(NamedTextColor.RED));
                plugin.getLogger().error("Error in validatetoken command", e);
            }
        });
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player) {
            return ((Player) source).hasPermission("bungeesystem.validatetoken");
        }
        return true; // Console always has permission
    }
}