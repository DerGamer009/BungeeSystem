package me.dergamer09.bungeesystem.listeners;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerEventListener implements Listener {

    @EventHandler
    public void onLogin(PostLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();

        try (PreparedStatement ps = BungeeSystem.getInstance().getDatabaseManager().getConnection().prepareStatement(
                "INSERT INTO online_time (player_uuid, total_time, last_login) VALUES (?, 0, ?) " +
                        "ON DUPLICATE KEY UPDATE last_login = ?")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, now);
            ps.setLong(3, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();

        try (PreparedStatement ps = BungeeSystem.getInstance().getDatabaseManager().getConnection().prepareStatement(
                "UPDATE online_time SET total_time = total_time + (? - last_login), last_login = 0 WHERE player_uuid = ?")) {
            ps.setLong(1, now);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
