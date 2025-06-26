package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;

/**
 * Simple ping command for Velocity.
 */
public class PingCommand implements SimpleCommand {
    private final ConfigManager configManager;

    public PingCommand(VelocitySystem plugin) {
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player) {
            Player player = (Player) source;
            Component msg = configManager.getMessageComponent("system.ping", "ping", String.valueOf(player.getPing()));
            player.sendMessage(msg);
        } else {
            source.sendMessage(configManager.getMessageComponent("general.player_only"));
        }
    }
}
