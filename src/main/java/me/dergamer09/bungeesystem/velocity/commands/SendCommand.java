package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

/**
 * Sends a player to a specified server.
 */
public class SendCommand implements SimpleCommand {
    private final ProxyServer server;
    private final ConfigManager configManager;

    public SendCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length < 2) {
            source.sendMessage(Component.text(configManager.getMessage("send.usage")));
            return;
        }
        String targetName = args[0];
        String serverName = args[1];

        Player target = server.getPlayer(targetName).orElse(null);
        RegisteredServer registeredServer = server.getServer(serverName).orElse(null);

        if (target != null && registeredServer != null) {
            target.createConnectionRequest(registeredServer).fireAndForget();
            String msg = configManager.getMessage("send.success", "player", targetName, "server", serverName);
            source.sendMessage(Component.text(msg));
        } else {
            source.sendMessage(Component.text(configManager.getMessage("send.player_or_server_not_found")));
        }
    }
}
