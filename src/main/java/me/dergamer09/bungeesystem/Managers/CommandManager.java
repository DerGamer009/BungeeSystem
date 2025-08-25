package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.commands.*;
import net.md_5.bungee.api.plugin.PluginManager;

/**
 * Manages command registration for the BungeeSystem plugin
 */
public class CommandManager {

    private final BungeeSystem plugin;
    
    public CommandManager(BungeeSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Registers all plugin commands
     */
    public void registerCommands() {
        PluginManager pm = plugin.getProxy().getPluginManager();
        
        // Basic commands
        pm.registerCommand(plugin, new TeamChatCommand());
        pm.registerCommand(plugin, new JoinMeCommand("joinme", "bungeesystem.joinme.use"));
        pm.registerCommand(plugin, new PingCommand());
        pm.registerCommand(plugin, new ServerListCommand());
        pm.registerCommand(plugin, new FindCommand());
        pm.registerCommand(plugin, new PlayCommand());
        pm.registerCommand(plugin, new ServerCommand());
        pm.registerCommand(plugin, new UptimeCommand());
        
        // Communication commands
        pm.registerCommand(plugin, new MSGCommand());
        pm.registerCommand(plugin, new ReplyCommand());
        pm.registerCommand(plugin, new IgnoreCommand());
        pm.registerCommand(plugin, new BroadcastCommand());
        pm.registerCommand(plugin, new GlobalChatCommand());
        pm.registerCommand(plugin, new StaffChatCommand());
        
        // Server management commands
        pm.registerCommand(plugin, new SendCommand());
        pm.registerCommand(plugin, new MaintenanceCommand());
        pm.registerCommand(plugin, new BlockBungeeCommand(plugin));
        pm.registerCommand(plugin, new ListCommand(plugin));
        pm.registerCommand(plugin, new ToggleNotifyCommand());
        pm.registerCommand(plugin, new OnlineTimeCommand());
        pm.registerCommand(plugin, new VanishCommand());
        
        // Teleport/server commands
        pm.registerCommand(plugin, new LobbyCommand());
        
        // System and admin commands
        pm.registerCommand(plugin, new ReloadConfigCommand());
        pm.registerCommand(plugin, new RestartCommand());
        
        // Quality-of-life commands
        pm.registerCommand(plugin, new AfkCommand());
        pm.registerCommand(plugin, new SeenCommand());
        pm.registerCommand(plugin, new NickCommand());
        pm.registerCommand(plugin, new WhoisCommand());
        
        // Statistics commands
        pm.registerCommand(plugin, new StatsCommand());
        pm.registerCommand(plugin, new TopCommand());
        
        // Moderation commands
        pm.registerCommand(plugin, new BanCommand(plugin));
        pm.registerCommand(plugin, new UnbanCommand(plugin));
        pm.registerCommand(plugin, new MuteCommand(plugin));
        pm.registerCommand(plugin, new UnmuteCommand(plugin));
        pm.registerCommand(plugin, new KickCommand(plugin));
        pm.registerCommand(plugin, new WarnCommand(plugin));
        pm.registerCommand(plugin, new ReportCommand(plugin));
        pm.registerCommand(plugin, new ReportsCommand(plugin));

    }
} 