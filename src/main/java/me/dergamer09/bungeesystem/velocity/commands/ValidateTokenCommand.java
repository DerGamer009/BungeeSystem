package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Command to validate the server token with the backend (Velocity version)
 */
public class ValidateTokenCommand implements SimpleCommand {

    private final VelocitySystem plugin;
    private final ProxyServer server;

    public ValidateTokenCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Check permission
        if (!source.hasPermission("bungeesystem.admin.validatetoken")) {
            source.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return;
        }

        // Check if API is enabled
        if (!plugin.getApiManager().isApiEnabled()) {
            source.sendMessage(Component.text("❌ API is disabled or no token configured!", NamedTextColor.RED));
            return;
        }

        // Start validation
        source.sendMessage(Component.text("🔍 Validating server token with backend...", NamedTextColor.YELLOW));
        
        // Run validation asynchronously
        server.getScheduler().buildTask(plugin, () -> {
            boolean isValid = plugin.getApiManager().validateServerToken();
            
            // Send result back to sender
            server.getScheduler().buildTask(plugin, () -> {
                if (isValid) {
                    source.sendMessage(Component.text("✅ Server token validation successful!", NamedTextColor.GREEN));
                    source.sendMessage(Component.text("Your server is now connected to the dashboard.", NamedTextColor.AQUA));
                } else {
                    source.sendMessage(Component.text("❌ Server token validation failed!", NamedTextColor.RED));
                    source.sendMessage(Component.text("Check your token configuration and try again.", NamedTextColor.YELLOW));
                }
            }).schedule();
        }).schedule();
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bungeesystem.admin.validatetoken");
    }
} 