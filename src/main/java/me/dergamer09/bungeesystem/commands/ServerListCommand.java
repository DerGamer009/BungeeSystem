package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.util.Map;

public class ServerListCommand extends Command {

    public ServerListCommand() {
        super("serverlist");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        StringBuilder servers = new StringBuilder(ChatColor.AQUA + "Online Server: ");
        Map<String, ServerInfo> serverMap = BungeeSystem.getInstance().getProxy().getServers();

        for (String server : serverMap.keySet()) {
            servers.append(ChatColor.YELLOW).append(server).append(ChatColor.GRAY).append(", ");
        }
        sender.sendMessage(servers.toString());
    }

}
