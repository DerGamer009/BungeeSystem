package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.io.IOException;

public class ReloadConfigCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;

    public ReloadConfigCommand() {
        super("reloadconfig", "bungeesystem.admin.reload");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            configManager.reloadAll();
            sender.sendMessage(new TextComponent(configManager.getMessage("system.reload_success")));
        } catch (IOException e) {
            sender.sendMessage(new TextComponent(
                    configManager.getMessage("system.reload_failed", "error", e.getMessage())
            ));
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 