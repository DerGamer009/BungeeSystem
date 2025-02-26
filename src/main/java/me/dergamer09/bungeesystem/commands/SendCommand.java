package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class SendCommand extends Command {

    public SendCommand() {
        super("send", "bungeesystem.send");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /send <player> <server>");
            return;
        }

        ProxiedPlayer target = BungeeSystem.getInstance().getProxy().getPlayer(args[0]);
        ServerInfo server = BungeeSystem.getInstance().getProxy().getServerInfo(args[1]);

        if (target != null && server != null) {
            target.connect(server);
            sender.sendMessage(ChatColor.GREEN + "Sent " + target.getName() + " to " + server.getName());
        } else {
            sender.sendMessage(ChatColor.RED + "Player or server not found!");
        }
    }
}

