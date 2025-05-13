package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Runnables.OnlineTimeUpdater;
import me.dergamer09.bungeesystem.listeners.ChatListener;
import me.dergamer09.bungeesystem.listeners.MotdListener;
import me.dergamer09.bungeesystem.listeners.PlayerEventListener;
import me.dergamer09.bungeesystem.listeners.PunishmentListener;
import me.dergamer09.bungeesystem.listeners.StatsListener;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.concurrent.TimeUnit;

/**
 * Manages listener registration and schedules tasks for the BungeeSystem plugin
 */
public class ListenerManager {

    private final BungeeSystem plugin;
    
    public ListenerManager(BungeeSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Registers all event listeners
     */
    public void registerListeners() {
        PluginManager pm = plugin.getProxy().getPluginManager();
        
        // Register listeners
        pm.registerListener(plugin, new MotdListener());
        pm.registerListener(plugin, new PlayerEventListener());
        pm.registerListener(plugin, new StatsListener(plugin));
        pm.registerListener(plugin, new ChatListener(plugin));
        pm.registerListener(plugin, new PunishmentListener(plugin));
    }
    
    /**
     * Schedules recurring tasks
     */
    public void scheduleRecurringTasks() {
        // Schedule online time updater
        plugin.getProxy().getScheduler().schedule(
            plugin, 
            new OnlineTimeUpdater(), 
            1L, 
            1L, 
            TimeUnit.SECONDS
        );
    }
} 