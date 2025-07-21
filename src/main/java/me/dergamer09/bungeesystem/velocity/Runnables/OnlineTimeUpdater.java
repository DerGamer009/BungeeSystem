package me.dergamer09.bungeesystem.velocity.Runnables;

import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;

import java.sql.PreparedStatement;

public class OnlineTimeUpdater implements Runnable {
    private final VelocitySystem plugin;

    public OnlineTimeUpdater(VelocitySystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getAllPlayers()) {
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(
                    "UPDATE online_time SET total_time = total_time + 1000 WHERE player_uuid = ?")) {
                ps.setString(1, player.getUniqueId().toString());
                ps.executeUpdate();
            } catch (Exception ignored) {
            }
        }
    }
}
