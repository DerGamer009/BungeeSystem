package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

/**
 * Displays the proxy uptime in seconds.
 */
public class UptimeCommand implements SimpleCommand {
    private final long startTime;
    private final ConfigManager configManager;

    public UptimeCommand(VelocitySystem plugin) {
        this.startTime = System.currentTimeMillis();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        String msg = configManager.getMessage("uptime.server_uptime", "seconds", String.valueOf(uptime));
        source.sendMessage(Component.text(msg));
    }
}
