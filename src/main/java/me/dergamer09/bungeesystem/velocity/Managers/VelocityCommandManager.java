package me.dergamer09.bungeesystem.velocity.Managers;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.VersionCommand;
import me.dergamer09.bungeesystem.velocity.commands.*;

/**
 * Registers placeholder commands for Velocity.
 */
public class VelocityCommandManager {
    private final VelocitySystem plugin;

    public VelocityCommandManager(VelocitySystem plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        com.velocitypowered.api.command.CommandManager cm = plugin.getServer().getCommandManager();
        cm.register(cm.metaBuilder("bsversion").plugin(plugin).build(), new VersionCommand(plugin, plugin.getVersion()));
        cm.register(cm.metaBuilder("ping").plugin(plugin).build(), new PingCommand(plugin));
        
        // API-related commands
        cm.register(cm.metaBuilder("generatetoken").plugin(plugin).build(), new GenerateTokenCommand(plugin));
        cm.register(cm.metaBuilder("validatetoken").plugin(plugin).build(), new ValidateTokenCommand(plugin));

        // Register placeholders for other commands
        registerPlaceholder(cm, "afk", new AfkCommand(plugin));
        registerPlaceholder(cm, "ban", new BanCommand());
        registerPlaceholder(cm, "blockbungee", new BlockBungeeCommand(plugin));
        registerPlaceholder(cm, "broadcast", new BroadcastCommand(plugin));
        registerPlaceholder(cm, "find", new FindCommand(plugin));
        registerPlaceholder(cm, "globalchat", new GlobalChatCommand());
        registerPlaceholder(cm, "ignore", new IgnoreCommand(plugin));
        registerPlaceholder(cm, "joinme", new JoinMeCommand());
        registerPlaceholder(cm, "list", new ListCommand(plugin));
        registerPlaceholder(cm, "lobby", new LobbyCommand(plugin));
        registerPlaceholder(cm, "l", new LobbyCommand(plugin));
        registerPlaceholder(cm, "hub", new LobbyCommand(plugin));
        registerPlaceholder(cm, "msg", new MSGCommand(plugin));
        registerPlaceholder(cm, "maintenance", new MaintenanceCommand(plugin));
        registerPlaceholder(cm, "mute", new MuteCommand());
        registerPlaceholder(cm, "nick", new NickCommand(plugin));
        registerPlaceholder(cm, "onlinetime", new OnlineTimeCommand());
        registerPlaceholder(cm, "play", new PlayCommand());
        registerPlaceholder(cm, "reloadconfig", new ReloadConfigCommand(plugin));
        registerPlaceholder(cm, "reply", new ReplyCommand(plugin));
        registerPlaceholder(cm, "report", new ReportCommand(plugin));
        registerPlaceholder(cm, "reports", new ReportsCommand(plugin));
        registerPlaceholder(cm, "restart", new RestartCommand());
        registerPlaceholder(cm, "seen", new SeenCommand(plugin));
        registerPlaceholder(cm, "send", new SendCommand(plugin));
        registerPlaceholder(cm, "server", new ServerCommand(plugin));
        registerPlaceholder(cm, "serverlist", new ServerListCommand(plugin));
        registerPlaceholder(cm, "staffchat", new StaffChatCommand());
        registerPlaceholder(cm, "stats", new StatsCommand(plugin));
        registerPlaceholder(cm, "teamchat", new TeamChatCommand());
        registerPlaceholder(cm, "togglenotify", new ToggleNotifyCommand());
        registerPlaceholder(cm, "top", new TopCommand(plugin));
        registerPlaceholder(cm, "unban", new UnbanCommand());
        registerPlaceholder(cm, "unmute", new UnmuteCommand());
        registerPlaceholder(cm, "uptime", new UptimeCommand(plugin));
        registerPlaceholder(cm, "vanish", new VanishCommand());
        registerPlaceholder(cm, "warn", new WarnCommand());
        registerPlaceholder(cm, "whois", new WhoisCommand());
        registerPlaceholder(cm, "generatetoken", new GenerateTokenCommand(plugin));
        registerPlaceholder(cm, "validatetoken", new ValidateTokenCommand(plugin));
    }

    private void registerPlaceholder(CommandManager cm, String name, SimpleCommand command) {
        cm.register(cm.metaBuilder(name).plugin(plugin).build(), command);
    }
}
