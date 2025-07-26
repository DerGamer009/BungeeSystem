package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Displays the proxy uptime in seconds.
 */
public class UptimeCommand implements SimpleCommand {
    private final long startTime;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public UptimeCommand(VelocitySystem plugin) {
        this.startTime = System.currentTimeMillis();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        long uptimeMillis = System.currentTimeMillis() - startTime;
        long seconds = (uptimeMillis / 1000) % 60;
        long minutes = (uptimeMillis / (1000 * 60)) % 60;
        long hours = (uptimeMillis / (1000 * 60 * 60)) % 24;
        long days = (uptimeMillis / (1000 * 60 * 60 * 24));
        String formatted = (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "m " : "") + seconds + "s";
        String restartTime = dateFormat.format(new Date(startTime));
        String msg = configManager.getMessage("uptime.server_uptime_full", "uptime", formatted, "restart", restartTime);
        source.sendMessage(Component.text(msg));
    }
}
