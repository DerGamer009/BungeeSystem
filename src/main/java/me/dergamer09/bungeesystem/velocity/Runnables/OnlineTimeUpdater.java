package me.dergamer09.bungeesystem.velocity.Runnables;

import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;

public class OnlineTimeUpdater implements Runnable {
    private final VelocitySystem plugin;

    public OnlineTimeUpdater(VelocitySystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getAllPlayers()) {
            // Placeholder: update stats for player
        }
    }
}
