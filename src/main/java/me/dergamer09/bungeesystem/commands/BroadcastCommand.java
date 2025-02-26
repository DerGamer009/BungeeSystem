package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class BroadcastCommand extends Command {

    public BroadcastCommand(){
        super("broadcast", "bungeesystem.broadcast");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /broadcast <message>");
            return;
        }

        String message = String.join(" ", args);
        BungeeSystem.getInstance().getProxy().broadcast(ChatColor.GOLD + "[Broadcast] " + ChatColor.WHITE + message);
    }

}
