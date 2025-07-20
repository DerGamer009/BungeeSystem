package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class MaintenanceCommand implements SimpleCommand {
    private static boolean maintenanceMode;
    private final ProxyServer server;
    private final ConfigManager configManager;

    public MaintenanceCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
        maintenanceMode = configManager.getConfig().getBoolean("maintenance.enabled", false);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            toggleMaintenance(sender);
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            String key = maintenanceMode ? "system.maintenance_status_on" : "system.maintenance_status_off";
            sender.sendMessage(Component.text(configManager.getMessage(key)));
            return;
        }

        if (args[0].equalsIgnoreCase("on")) {
            setMaintenance(sender, true);
            return;
        }
        if (args[0].equalsIgnoreCase("off")) {
            setMaintenance(sender, false);
            return;
        }
        if (args[0].equalsIgnoreCase("whitelist") && args.length >= 3) {
            handleWhitelist(sender, args);
            return;
        }

        sender.sendMessage(Component.text("§eUsage: §f/maintenance [on|off]"));
        sender.sendMessage(Component.text("§eUsage: §f/maintenance whitelist add <player>"));
        sender.sendMessage(Component.text("§eUsage: §f/maintenance whitelist remove <player>"));
    }

    private void toggleMaintenance(CommandSource sender) {
        maintenanceMode = !maintenanceMode;
        configManager.getConfig().set("maintenance.enabled", maintenanceMode);
        configManager.saveConfig();
        if (maintenanceMode) {
            sender.sendMessage(Component.text(configManager.getMessage("system.maintenance_enabled")));
            kickNonWhitelisted();
        } else {
            sender.sendMessage(Component.text(configManager.getMessage("system.maintenance_disabled")));
        }
    }

    private void setMaintenance(CommandSource sender, boolean enabled) {
        maintenanceMode = enabled;
        configManager.getConfig().set("maintenance.enabled", enabled);
        configManager.saveConfig();
        if (enabled) {
            sender.sendMessage(Component.text(configManager.getMessage("system.maintenance_enabled")));
            kickNonWhitelisted();
        } else {
            sender.sendMessage(Component.text(configManager.getMessage("system.maintenance_disabled")));
        }
    }

    private void handleWhitelist(CommandSource sender, String[] args) {
        if (args[1].equalsIgnoreCase("add") && args.length >= 3) {
            Player target = server.getPlayer(args[2]).orElse(null);
            if (target == null) {
                sender.sendMessage(Component.text(configManager.getMessage("system.player_not_found", "player", args[2])));
                return;
            }
            UUID uuid = target.getUniqueId();
            configManager.addToWhitelist(uuid);
            sender.sendMessage(Component.text(configManager.getMessage("whitelist.player_added", "player", target.getUsername())));
            return;
        }
        if (args[1].equalsIgnoreCase("remove") && args.length >= 3) {
            Player target = server.getPlayer(args[2]).orElse(null);
            if (target == null) {
                sender.sendMessage(Component.text(configManager.getMessage("system.player_not_found", "player", args[2])));
                return;
            }
            UUID uuid = target.getUniqueId();
            configManager.removeFromWhitelist(uuid);
            sender.sendMessage(Component.text(configManager.getMessage("whitelist.player_removed", "player", target.getUsername())));
            if (maintenanceMode && target.isActive()) {
                target.disconnect(Component.text(configManager.getMessage("join.maintenance_kick") + "\n" +
                        configManager.getMessage("join.maintenance_kick_info")));
            }
        }
    }

    private void kickNonWhitelisted() {
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("bungeesystem.maintenance.bypass")) {
                continue;
            }
            if (!configManager.isInWhitelist(player.getUniqueId())) {
                player.disconnect(Component.text(
                        configManager.getMessage("join.maintenance_kick") + "\n" +
                        configManager.getMessage("join.maintenance_kick_info")
                ));
            }
        }
    }

    public static boolean isMaintenanceMode() {
        return maintenanceMode;
    }
}
