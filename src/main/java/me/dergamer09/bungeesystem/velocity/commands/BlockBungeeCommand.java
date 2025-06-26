package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import net.kyori.adventure.text.Component;

/**
 * Demonstrates permission checks and prints the proxy version.
 */
public class BlockBungeeCommand implements SimpleCommand {
    private final ProxyServer server;
    private final ConfigManager configManager;

    public BlockBungeeCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source.hasPermission("bungeesystem.allow.bungee")) {
            String msg = configManager.getMessage(
                    "blockbungee.allowed",
                    "version",
                    server.getVersion().getVersion()
            );
            source.sendMessage(Component.text(msg));
        } else if (source.hasPermission("bungeesystem.notallowd.bungee")) {
            source.sendMessage(Component.text(configManager.getMessage("blockbungee.not_allowed")));
        } else {
            source.sendMessage(Component.text(configManager.getMessage("general.no_permission")));
        }
    }
}
