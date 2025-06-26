package me.dergamer09.bungeesystem.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import net.kyori.adventure.text.Component;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;

/**
 * Simple command to display the current plugin version on Velocity.
 */
public class VersionCommand implements SimpleCommand {

    private final String version;
    private final ConfigManager configManager;
    public VersionCommand(VelocitySystem plugin, String version) {
        this.version = version;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String message = configManager.getMessage("system.version", "version", version);
        source.sendMessage(Component.text(message));
    }
}
