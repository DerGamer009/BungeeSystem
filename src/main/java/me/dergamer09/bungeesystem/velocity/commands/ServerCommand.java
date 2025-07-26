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
        String[] args = invocation.arguments();
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
            source.sendMessage(Component.text("§bAvailable Servers:"));
            server.getAllServers().forEach(s -> {
                String name = s.getServerInfo().getName();
                Component clickable = Component.text("§e" + name)
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Click to connect to " + name)))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/server " + name));
                source.sendMessage(clickable);
            });
            return;
        }
        // Try to connect if player
        if (source instanceof com.velocitypowered.api.proxy.Player) {
            String serverName = args[0];
            server.getServer(serverName).ifPresentOrElse(
                reg -> {
                    com.velocitypowered.api.proxy.Player player = (com.velocitypowered.api.proxy.Player) source;
                    player.createConnectionRequest(reg).connect();
                    source.sendMessage(Component.text("§aConnecting you to §e" + serverName + "§a..."));
                },
                () -> source.sendMessage(Component.text("§cServer not found!"))
            );
        } else {
            source.sendMessage(Component.text("§cThis command can only be used by players!"));
        }
    }
}
