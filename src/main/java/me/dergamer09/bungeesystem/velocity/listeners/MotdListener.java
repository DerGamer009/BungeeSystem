package me.dergamer09.bungeesystem.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.MotdManager;
import me.dergamer09.bungeesystem.velocity.commands.MaintenanceCommand;
import net.kyori.adventure.text.Component;

/**
 * Listens for proxy ping events to set the MOTD.
 */
public class MotdListener {
    private final VelocitySystem plugin;

    public MotdListener(VelocitySystem plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        MotdManager manager = plugin.getMotdManager();
        String motd = manager.getMotd(MaintenanceCommand.isMaintenanceMode());
        ServerPing ping = event.getPing();
        ServerPing.Builder builder = ping.asBuilder().description(Component.text(motd));
        event.setPing(builder.build());
    }
}
