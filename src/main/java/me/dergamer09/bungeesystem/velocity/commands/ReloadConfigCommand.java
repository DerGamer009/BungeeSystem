package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;

import java.io.IOException;

public class ReloadConfigCommand implements SimpleCommand {
    private final VelocitySystem plugin;
    private final ConfigManager configManager;

    public ReloadConfigCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        try {
            configManager.reloadAll();
            plugin.getMotdManager().loadFromConfig();
            source.sendMessage(configManager.getMessageComponent("system.reload_success"));
        } catch (IOException e) {
            source.sendMessage(configManager.getMessageComponent(
                    "system.reload_failed", "error", e.getMessage()
            ));
        }
    }
}
