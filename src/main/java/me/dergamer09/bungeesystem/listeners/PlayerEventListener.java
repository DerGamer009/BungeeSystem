package me.dergamer09.bungeesystem.listeners;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class PlayerEventListener implements Listener {

    private final BungeeSystem plugin;

    public PlayerEventListener(BungeeSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        try (Connection connection = plugin.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO online_time (player_name, login_time) VALUES (?, ?) ON DUPLICATE KEY UPDATE login_time = ?")) {

            long currentTime = System.currentTimeMillis();
            statement.setString(1, player.getName());
            statement.setLong(2, currentTime);
            statement.setLong(3, currentTime);
            statement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Speichern der Anmeldezeit in der Datenbank: ", e);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        try (Connection connection = plugin.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM online_time WHERE player_name = ?")) {

            statement.setString(1, player.getName());
            statement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Entfernen der Abmeldezeit aus der Datenbank: ", e);
        }
    }
}
