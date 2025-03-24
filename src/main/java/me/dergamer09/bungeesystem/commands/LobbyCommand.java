package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

import java.util.List;
import java.util.Random;

public class LobbyCommand extends Command {

    public LobbyCommand(String name) {
        super(name);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        Configuration config = BungeeSystem.getInstance().getConfig();
        List<String> lobbies = config.getStringList("lobbies");

        if (lobbies == null || lobbies.isEmpty()) {
            player.sendMessage("§cNo lobbies have been defined in the config.yml.");
            return;
        }

        // Check if the player is already on a lobby server
        if (lobbies.contains(player.getServer().getInfo().getName())) {
            player.sendMessage("§eYou are already connected to a lobby.");
            return;
        }

        // Choose a random lobby
        String targetLobby = lobbies.get(new Random().nextInt(lobbies.size()));
        ServerInfo lobbyServer = BungeeSystem.getInstance().getProxy().getServerInfo(targetLobby);

        if (lobbyServer != null) {
            player.connect(lobbyServer);
            player.sendMessage("§aConnecting you to the lobby §e" + targetLobby + "§a...");
        } else {
            player.sendMessage("§cThe lobby §e" + targetLobby + " §ccould not be found.");
        }
    }
}
