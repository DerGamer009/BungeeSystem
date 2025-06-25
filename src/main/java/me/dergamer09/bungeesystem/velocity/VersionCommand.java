package me.dergamer09.bungeesystem.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import net.kyori.adventure.text.Component;

/**
 * Simple command to display the current plugin version on Velocity.
 */
public class VersionCommand implements SimpleCommand {

    private final String version;

    public VersionCommand(String version) {
        this.version = version;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        source.sendMessage(Component.text("BungeeSystem version " + version));
    }
}
