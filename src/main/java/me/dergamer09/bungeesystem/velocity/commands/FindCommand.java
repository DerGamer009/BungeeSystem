package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

/**
 * Finds the server a player is connected to.
 */
public class FindCommand implements SimpleCommand {
    private final ProxyServer server;
    private final ConfigManager configManager;

    public FindCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length < 1) {
            source.sendMessage(Component.text(configManager.getMessage("find.usage")));
            return;
        }
        String playerName = args[0];
        Player target = server.getPlayer(playerName).orElse(null);
        if (target != null && target.getCurrentServer().isPresent()) {
            String serverName = target.getCurrentServer().get().getServerInfo().getName();
            String msg = configManager.getMessage("find.player_location", "player", playerName, "server", serverName);
            source.sendMessage(Component.text(msg));
        } else {
            source.sendMessage(Component.text(configManager.getMessage("find.player_not_found", "player", playerName)));
        }
    }
}
