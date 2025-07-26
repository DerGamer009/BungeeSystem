package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
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
            TextComponent header = new TextComponent(ChatColor.AQUA + "Available Servers: ");
            sender.sendMessage(header);
            for (String server : serverMap.keySet()) {
                TextComponent serverComponent = new TextComponent(ChatColor.YELLOW + server);
                serverComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.ComponentBuilder("Click to connect to " + server).create()
                ));
                serverComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/server " + server
                ));
                sender.sendMessage(serverComponent);
            }
            return;
        }

        // Connect sender to target server
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return;
        }

        ProxiedPlayer p = (ProxiedPlayer) sender;
        if (MaintenanceCommand.isMaintenanceMode()
                && !p.hasPermission("bungeesystem.maintenance.bypass")
                && !BungeeSystem.getInstance().getConfigManager().isInWhitelist(p.getUniqueId())) {
            p.disconnect(new TextComponent(BungeeSystem.getInstance().getConfigManager().getMessage("join.maintenance_kick")
                    + "\n" + BungeeSystem.getInstance().getConfigManager().getMessage("join.maintenance_kick_info")));
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
