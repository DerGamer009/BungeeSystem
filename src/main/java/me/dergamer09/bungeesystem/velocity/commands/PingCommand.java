package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

/**
 * Simple ping command for Velocity.
 */
public class PingCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player) {
            Player player = (Player) source;
            player.sendMessage(Component.text("Pong: " + player.getPing() + "ms"));
        } else {
            source.sendMessage(Component.text("This command can only be used by players."));
        }
    }
}
