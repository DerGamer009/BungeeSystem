package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashSet;
import java.util.Set;

public class ToggleNotifyCommand extends Command {

    private final String prefix = ChatColor.DARK_GRAY + "| " + ChatColor.RED + "ᴍɪɴᴇᴄᴏꜱɪᴀ " + ChatColor.GRAY + "» ";
    private final Set<String> notifiedPlayers = new HashSet<>();

    public ToggleNotifyCommand() {
        super("togglenotify", "bungeesystem.notify", "tnotify");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(prefix + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        String playerName = player.getName();

        if (notifiedPlayers.contains(playerName)) {
            notifiedPlayers.remove(playerName);
            player.sendMessage(prefix + ChatColor.GREEN + "Benachrichtigungen wurden " + ChatColor.RED + "deaktiviert" + ChatColor.GREEN + ".");
        } else {
            notifiedPlayers.add(playerName);
            player.sendMessage(prefix + ChatColor.GREEN + "Benachrichtigungen wurden " + ChatColor.YELLOW + "aktiviert" + ChatColor.GREEN + ".");
        }
    }

    public boolean hasNotificationsEnabled(ProxiedPlayer player) {
        return notifiedPlayers.contains(player.getName());
    }
}
