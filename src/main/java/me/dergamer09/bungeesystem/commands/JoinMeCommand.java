package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class JoinMeCommand extends Command {

    public JoinMeCommand(String name, String permission) {
        super(name, permission);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!player.hasPermission("joinme.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        // Get the server the player is currently on
        String serverName = player.getServer().getInfo().getName();

        // Broadcast the join message to all players
        for (ProxiedPlayer p : player.getServer().getInfo().getPlayers()) {
            p.sendMessage(ChatColor.AQUA + "[JoinMe] " + ChatColor.GREEN + player.getName() + " is playing on " + ChatColor.YELLOW + serverName + ChatColor.GREEN + "! Use /server " + serverName + " to join them!");
        }
    }
}
