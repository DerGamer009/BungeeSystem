package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OnlineTimeCommand extends Command {

    private final BungeeSystem plugin;
    private final Map<String, Long> loginTimes = new HashMap<>();

    public OnlineTimeCommand(BungeeSystem plugin) {
        super("onlinetime", "bungeesystem.onlinetime", "otime");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getErrorMessageColor() + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        String playerName = player.getName();

        if (!loginTimes.containsKey(playerName)) {
            player.sendMessage(plugin.getPrefix() + plugin.getErrorMessageColor() + "Es konnte keine Online-Zeit gefunden werden.");
            return;
        }

        long loginTime = loginTimes.get(playerName);
        long currentTime = System.currentTimeMillis();
        long onlineTimeMillis = currentTime - loginTime;

        String formattedTime = formatTime(onlineTimeMillis);
        player.sendMessage(plugin.getPrefix() + plugin.getSuccessMessageColor() + "Deine bisherige Online-Zeit: " + plugin.getUpdateMessageColor() + formattedTime);
    }

    public void recordLoginTime(ProxiedPlayer player) {
        loginTimes.put(player.getName(), System.currentTimeMillis());
    }

    public void recordLogoutTime(ProxiedPlayer player) {
        loginTimes.remove(player.getName());
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));

        return String.format("%02d Stunden, %02d Minuten, %02d Sekunden", hours, minutes, seconds);
    }
}
