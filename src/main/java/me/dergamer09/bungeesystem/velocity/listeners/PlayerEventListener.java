package me.dergamer09.bungeesystem.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.commands.MaintenanceCommand;
import net.kyori.adventure.text.Component;

public class PlayerEventListener {
    private final VelocitySystem plugin;

    public PlayerEventListener(VelocitySystem plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (MaintenanceCommand.isMaintenanceMode()
                && !player.hasPermission("bungeesystem.maintenance.bypass")
                && !plugin.getDatabaseManager().isInWhitelist(player.getUniqueId())) {
            player.disconnect(Component.text(
                    plugin.getConfigManager().getMessage("join.maintenance_kick") + "\n" +
                            plugin.getConfigManager().getMessage("join.maintenance_kick_info")));
        }
    }
}
