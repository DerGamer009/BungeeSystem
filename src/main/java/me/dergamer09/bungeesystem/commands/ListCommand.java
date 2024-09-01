package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import java.util.stream.Collectors;

public class ListCommand extends Command {

    private final Plugin plugin;
    private final String prefix = ChatColor.DARK_GRAY + "| " + ChatColor.RED + "ᴍɪɴᴇᴄᴏꜱɪᴀ " + ChatColor.GRAY + "» ";

    public ListCommand(Plugin plugin) {
        super("list");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(prefix + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        String playersList = plugin.getProxy().getPlayers().stream()
                .map(ProxiedPlayer::getName)
                .collect(Collectors.joining(", "));

        player.sendMessage(prefix + ChatColor.GREEN + "Derzeit online: " + ChatColor.YELLOW + playersList);
    }
}

