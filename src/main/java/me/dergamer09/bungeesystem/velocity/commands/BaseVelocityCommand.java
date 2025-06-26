package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;

/**
 * Basic placeholder implementation for ported Bungee commands on Velocity.
 */
public class BaseVelocityCommand implements SimpleCommand {
    private final String message;

    public BaseVelocityCommand(String message) {
        this.message = message;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        source.sendMessage(Component.text(message));
    }
}
