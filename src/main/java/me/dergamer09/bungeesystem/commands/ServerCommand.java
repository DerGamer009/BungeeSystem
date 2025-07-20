package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Map;

/**
 * Handles the /server command.
 *
 * <p>/server list</p> or <p>/server</p> lists all available servers.
 * <p>/server &lt;name&gt;</p> connects the executing player to the specified server.
 */
public class ServerCommand extends Command {

    public ServerCommand() {
        super("server", "bungeesystem.servers", "servers");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Map<String, ServerInfo> serverMap = BungeeSystem.getInstance().getProxy().getServers();

        // List servers when no argument or "list" is provided
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
            StringBuilder servers = new StringBuilder(ChatColor.AQUA + "Available Servers: ");
            for (String server : serverMap.keySet()) {
                servers.append(ChatColor.YELLOW).append(server).append(ChatColor.GRAY).append(", ");
            }
            if (servers.lastIndexOf(", ") == servers.length() - 2) {
                servers.setLength(servers.length() - 2);
            }
            sender.sendMessage(servers.toString());
            return;
        }

        // Connect sender to target server
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return;
        }

        String serverName = args[0];
        ServerInfo server = serverMap.get(serverName);

        if (server != null) {
            ((ProxiedPlayer) sender).connect(server);
            sender.sendMessage(ChatColor.GREEN + "Connecting you to " + ChatColor.YELLOW + serverName + ChatColor.GREEN + "...");
        } else {
            sender.sendMessage(ChatColor.RED + "Server not found!");
        }
    }
}
