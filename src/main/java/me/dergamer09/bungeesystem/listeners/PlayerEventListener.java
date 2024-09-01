package me.dergamer09.bungeesystem.listeners;

import me.dergamer09.bungeesystem.commands.OnlineTimeCommand;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.event.EventHandler;

public class PlayerEventListener implements Listener {

    private final OnlineTimeCommand onlineTimeCommand;

    public PlayerEventListener(OnlineTimeCommand onlineTimeCommand) {
        this.onlineTimeCommand = onlineTimeCommand;
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        onlineTimeCommand.recordLoginTime(player);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        onlineTimeCommand.recordLogoutTime(player);
    }
}