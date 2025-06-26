package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

import java.util.stream.Collectors;

/**
 * Lists all online players on the proxy.
 */
public class ListCommand implements SimpleCommand {
    private final ProxyServer server;
    private final ConfigManager configManager;

    public ListCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String players = server.getAllPlayers().stream()
                .map(Player::getUsername)
                .collect(Collectors.joining(", "));
        String msg = configManager.getMessage("list.online_players",
                "count", String.valueOf(server.getPlayerCount()),
                "players", players);
        source.sendMessage(Component.text(msg));
    }
}
