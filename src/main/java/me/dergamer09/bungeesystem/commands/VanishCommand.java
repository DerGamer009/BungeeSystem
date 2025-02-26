package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashSet;
import java.util.Set;

public class VanishCommand extends Command {

    private static final Set<ProxiedPlayer> vanishedPlayers = new HashSet<>();

    public VanishCommand() {
        super("vanish", "bungeesystem.vanish",  "v");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        if (vanishedPlayers.contains(player)) {
            vanishedPlayers.remove(player);
            sender.sendMessage(ChatColor.RED + "You are no longer vanished!");
        } else {
            vanishedPlayers.add(player);
            sender.sendMessage(ChatColor.GREEN + "You are now vanished!");
        }
    }

}
