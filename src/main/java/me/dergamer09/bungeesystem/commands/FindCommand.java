package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class FindCommand extends Command {
    public FindCommand() {
        super("find");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Benutzung: /find <Spieler>");
            return;
        }

        ProxiedPlayer target = BungeeSystem.getInstance().getProxy().getPlayer(args[0]);
        if (target != null && target.isConnected()) {
            sender.sendMessage(ChatColor.GREEN + target.getName() + " befindet sich auf " + ChatColor.YELLOW + target.getServer().getInfo().getName());
        } else {
            sender.sendMessage(ChatColor.RED + "Spieler nicht gefunden oder offline.");
        }
    }
}
