package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

import java.util.List;
import java.util.Random;

public class LobbyCommand extends Command {

    public LobbyCommand() {
        super("lobby", null, "l", "hub");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            BungeeSystem.getInstance().getConfigManager().getMessage("lobby.player_only");
            sender.sendMessage(BungeeSystem.getInstance().getConfigManager().getMessage("lobby.player_only"));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        ConfigManager configManager = BungeeSystem.getInstance().getConfigManager();
        Configuration config = BungeeSystem.getInstance().getConfig();
        List<String> lobbies = config.getStringList("lobbies");

        if (lobbies == null || lobbies.isEmpty()) {
            player.sendMessage(configManager.getMessage("lobby.no_lobbies"));
            return;
        }

        String currentLobby = player.getServer().getInfo().getName();
        if (lobbies.contains(currentLobby)) {
            player.sendMessage(configManager.getMessage("lobby.already_on_lobby", "lobby", currentLobby));
            return;
        }

        String targetLobby = lobbies.get(new Random().nextInt(lobbies.size()));
        ServerInfo lobbyServer = BungeeSystem.getInstance().getProxy().getServerInfo(targetLobby);

        if (lobbyServer != null) {
            player.connect(lobbyServer);
            player.sendMessage(configManager.getMessage("lobby.connecting", "lobby", targetLobby));
        } else {
            player.sendMessage(configManager.getMessage("lobby.not_found", "lobby", targetLobby));
        }
    }
}
