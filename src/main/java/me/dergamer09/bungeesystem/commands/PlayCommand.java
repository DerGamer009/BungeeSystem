package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PlayCommand extends Command {

    public PlayCommand() {
        super("play");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /play <player>");
            return;
        }

        String serverName = args[0];
        ServerInfo server = BungeeSystem.getInstance().getProxy().getServerInfo(serverName);

        if (server != null) {
            ((ProxiedPlayer) sender).connect(server);
            sender.sendMessage(ChatColor.GREEN + "You will be connected to the server " + ChatColor.YELLOW + serverName + ChatColor.GREEN + "...");
        } else  {
            sender.sendMessage(ChatColor.RED + "Server not found!");
        }

    }

}
