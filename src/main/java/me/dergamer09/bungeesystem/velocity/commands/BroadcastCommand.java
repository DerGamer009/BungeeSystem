package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

/**
 * Broadcast a message to all players on Velocity.
 */
public class BroadcastCommand implements SimpleCommand {
    private final ProxyServer server;
    private final ConfigManager configManager;

    public BroadcastCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length == 0) {
            source.sendMessage(Component.text(configManager.getMessage("broadcast.usage")));
            return;
        }
        String message = String.join(" ", args);
        String prefix = configManager.getMessage("broadcast.prefix");
        server.sendMessage(Component.text(prefix + message));
    }
}
