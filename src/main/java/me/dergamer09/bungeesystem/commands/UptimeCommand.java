package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class UptimeCommand extends Command {

    private final long startTime;

    public UptimeCommand() {
        super("uptime",  "bungeesystem.uptime");
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        sender.sendMessage(ChatColor.GREEN + "Server Uptime: " + uptime + " seconds");
    }

}
