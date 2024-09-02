package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class OnlineTimeCommand extends Command {

    private final BungeeSystem plugin;

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

        try (Connection connection = plugin.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT login_time FROM online_time WHERE player_name = ?")) {

            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                long loginTime = resultSet.getLong("login_time");
                long currentTime = System.currentTimeMillis();
                long onlineTimeMillis = currentTime - loginTime;

                String formattedTime = formatTime(onlineTimeMillis);
                player.sendMessage(plugin.getPrefix() + plugin.getSuccessMessageColor() + "Deine bisherige Online-Zeit: " + plugin.getUpdateMessageColor() + formattedTime);
            } else {
                player.sendMessage(plugin.getPrefix() + plugin.getErrorMessageColor() + "Es konnte keine Online-Zeit gefunden werden.");
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Abrufen der Online-Zeit aus der Datenbank: ", e);
            player.sendMessage(plugin.getPrefix() + plugin.getErrorMessageColor() + "Ein Fehler ist aufgetreten.");
        }
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));

        return String.format("%02d Stunden, %02d Minuten, %02d Sekunden", hours, minutes, seconds);
    }
}
