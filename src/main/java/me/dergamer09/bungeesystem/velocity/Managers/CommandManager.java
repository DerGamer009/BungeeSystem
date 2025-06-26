package me.dergamer09.bungeesystem.velocity.Managers;

import com.velocitypowered.api.command.CommandManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.VersionCommand;
import me.dergamer09.bungeesystem.velocity.commands.*;

/**
 * Registers placeholder commands for Velocity.
 */
public class CommandManager {
    private final VelocitySystem plugin;

    public CommandManager(VelocitySystem plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        var cm = plugin.getServer().getCommandManager();
        cm.register(cm.metaBuilder("bsversion").plugin(plugin).build(), new VersionCommand(plugin.getVersion()));
        cm.register(cm.metaBuilder("ping").plugin(plugin).build(), new PingCommand());

        // Register placeholders for other commands
        registerPlaceholder(cm, "afk", new AfkCommand());
        registerPlaceholder(cm, "ban", new BanCommand());
        registerPlaceholder(cm, "blockbungee", new BlockBungeeCommand());
        registerPlaceholder(cm, "broadcast", new BroadcastCommand());
        registerPlaceholder(cm, "find", new FindCommand());
        registerPlaceholder(cm, "globalchat", new GlobalChatCommand());
        registerPlaceholder(cm, "ignore", new IgnoreCommand());
        registerPlaceholder(cm, "joinme", new JoinMeCommand());
        registerPlaceholder(cm, "list", new ListCommand());
        registerPlaceholder(cm, "lobby", new LobbyCommand());
        registerPlaceholder(cm, "msg", new MSGCommand());
        registerPlaceholder(cm, "maintenance", new MaintenanceCommand());
        registerPlaceholder(cm, "mute", new MuteCommand());
        registerPlaceholder(cm, "nick", new NickCommand());
        registerPlaceholder(cm, "onlinetime", new OnlineTimeCommand());
        registerPlaceholder(cm, "play", new PlayCommand());
        registerPlaceholder(cm, "reloadconfig", new ReloadConfigCommand());
        registerPlaceholder(cm, "reply", new ReplyCommand());
        registerPlaceholder(cm, "report", new ReportCommand());
        registerPlaceholder(cm, "reports", new ReportsCommand());
        registerPlaceholder(cm, "restart", new RestartCommand());
        registerPlaceholder(cm, "seen", new SeenCommand());
        registerPlaceholder(cm, "send", new SendCommand());
        registerPlaceholder(cm, "server", new ServerCommand());
        registerPlaceholder(cm, "serverlist", new ServerListCommand());
        registerPlaceholder(cm, "staffchat", new StaffChatCommand());
        registerPlaceholder(cm, "stats", new StatsCommand());
        registerPlaceholder(cm, "teamchat", new TeamChatCommand());
        registerPlaceholder(cm, "togglenotify", new ToggleNotifyCommand());
        registerPlaceholder(cm, "top", new TopCommand());
        registerPlaceholder(cm, "unban", new UnbanCommand());
        registerPlaceholder(cm, "unmute", new UnmuteCommand());
        registerPlaceholder(cm, "uptime", new UptimeCommand());
        registerPlaceholder(cm, "vanish", new VanishCommand());
        registerPlaceholder(cm, "warn", new WarnCommand());
        registerPlaceholder(cm, "whois", new WhoisCommand());
    }

    private void registerPlaceholder(CommandManager cm, String name, BaseVelocityCommand command) {
        cm.register(cm.metaBuilder(name).plugin(plugin).build(), command);
    }
}
