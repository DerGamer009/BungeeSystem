package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TeamChatCommand extends Command {

    public TeamChatCommand(String name, String permission) {
        super(name, permission);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!player.hasPermission("teamchat.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /teamchat <message>");
            return;
        }

        String message = String.join(" ", args);

        // Send message to all players with the permission "teamchat.receive"
        for (ProxiedPlayer p : player.getServer().getInfo().getPlayers()) {
            if (p.hasPermission("teamchat.receive")) {
                p.sendMessage(ChatColor.GREEN + "[TeamChat] " + player.getName() + ": " + ChatColor.YELLOW + message);
            }
        }
    }
}
