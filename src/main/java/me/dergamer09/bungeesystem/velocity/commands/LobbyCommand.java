package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Random;

public class LobbyCommand implements SimpleCommand {
    private final ProxyServer server;
    private final ConfigManager configManager;

    public LobbyCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            configManager.getMessageComponent("lobby.player_only");
            source.sendMessage(configManager.getMessageComponent("lobby.player_only"));
            return;
        }
        Player player = (Player) source;
        List<String> lobbies = configManager.getConfig().getStringList("lobbies");
        if (lobbies == null || lobbies.isEmpty()) {
            player.sendMessage(configManager.getMessageComponent("lobby.no_lobbies"));
            return;
        }
        String current = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "";
        if (lobbies.contains(current)) {
            player.sendMessage(configManager.getMessageComponent("lobby.already_on_lobby", "lobby", current));
            return;
        }
        String targetLobby = lobbies.get(new Random().nextInt(lobbies.size()));
        server.getServer(targetLobby).ifPresentOrElse(
            reg -> {
                player.createConnectionRequest(reg).connect();
                player.sendMessage(configManager.getMessageComponent("lobby.connecting", "lobby", targetLobby));
            },
            () -> player.sendMessage(configManager.getMessageComponent("lobby.not_found", "lobby", targetLobby))
        );
    }
}
