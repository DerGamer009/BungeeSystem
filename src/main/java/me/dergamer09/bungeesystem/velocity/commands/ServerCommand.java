package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

import java.util.stream.Collectors;

/**
 * Lists all known servers from the proxy configuration.
 */
public class ServerCommand implements SimpleCommand {
    private final ProxyServer server;
    private final ConfigManager configManager;

    public ServerCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String servers = server.getAllServers().stream()
                .map(s -> s.getServerInfo().getName())
                .collect(Collectors.joining(", "));
        String msg = configManager.getMessage("server.list", "servers", servers);
        source.sendMessage(Component.text(msg));
    }
}
