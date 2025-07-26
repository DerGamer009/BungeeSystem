package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UptimeCommand extends Command {

    private final long startTime;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public UptimeCommand() {
        super("uptime",  "bungeesystem.uptime");
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        long uptimeMillis = System.currentTimeMillis() - startTime;
        long seconds = (uptimeMillis / 1000) % 60;
        long minutes = (uptimeMillis / (1000 * 60)) % 60;
        long hours = (uptimeMillis / (1000 * 60 * 60)) % 24;
        long days = (uptimeMillis / (1000 * 60 * 60 * 24));
        String formatted = (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "m " : "") + seconds + "s";
        String restartTime = dateFormat.format(new Date(startTime));
        String msg = me.dergamer09.bungeesystem.BungeeSystem.getInstance().getConfigManager().getMessage("uptime.server_uptime_full", "uptime", formatted, "restart", restartTime);
        sender.sendMessage(msg);
    }

}
