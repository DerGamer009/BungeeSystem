package me.dergamer09.bungeesystem.Runnables;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class OnlineTimeUpdater implements Runnable {

    @Override
    public void run() {
        for (ProxiedPlayer player : BungeeSystem.getInstance().getProxy().getPlayers()) {
            UUID uuid = player.getUniqueId();

            try (PreparedStatement ps = BungeeSystem.getInstance().getDatabaseManager().getConnection().prepareStatement(
                    "UPDATE online_time SET total_time = total_time + 1000 WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
