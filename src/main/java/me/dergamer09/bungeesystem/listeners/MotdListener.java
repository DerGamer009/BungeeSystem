package me.dergamer09.bungeesystem.listeners;

import me.dergamer09.bungeesystem.commands.MaintenanceCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

// MOTD Listener
public class MotdListener implements Listener {
    @EventHandler
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing response = event.getResponse();

        if (MaintenanceCommand.isMaintenanceMode()) {
            response.setDescriptionComponent(new TextComponent(ChatColor.RED + "🚧 Maintenance Mode - Server is currently under maintenance! 🚧"));
        } else {
            response.setDescriptionComponent(new TextComponent(ChatColor.GREEN + "Welcome to the BungeeSystem Server!"));
        }
    }
}

