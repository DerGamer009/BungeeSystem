package me.dergamer09.bungeesystem.listeners;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.MotdManager;
import me.dergamer09.bungeesystem.commands.MaintenanceCommand;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

// MOTD Listener
public class MotdListener implements Listener {
    private final BungeeSystem plugin;

    public MotdListener(BungeeSystem plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing response = event.getResponse();
        MotdManager manager = plugin.getMotdManager();
        String motd = manager.getMotd(MaintenanceCommand.isMaintenanceMode());
        // Use MiniMessage to properly handle <center> and other formatting tags
        String legacyText = LegacyComponentSerializer.legacySection().serialize(
            MiniMessage.miniMessage().deserialize(motd)
        );
        response.setDescriptionComponent(new TextComponent(legacyText));
    }
}

