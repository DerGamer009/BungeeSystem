package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

public class MaintenanceCommand extends Command {

    private static boolean maintenanceMode;
    private final BungeeSystem plugin;
    private final ConfigManager configManager;

    public MaintenanceCommand() {
        super("maintenance", "bungeesystem.maintenance");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
        this.maintenanceMode = configManager.getConfig().getBoolean("maintenance.enabled", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Toggle maintenance mode
            maintenanceMode = !maintenanceMode;
            configManager.getConfig().set("maintenance.enabled", maintenanceMode);
            configManager.saveConfig();

            if (maintenanceMode) {
                sender.sendMessage(new TextComponent(configManager.getMessage("system.maintenance_enabled")));
                // Kick non-whitelisted players
                kickNonWhitelistedPlayers();
            } else {
                sender.sendMessage(new TextComponent(configManager.getMessage("system.maintenance_disabled")));
            }
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            String key = maintenanceMode ? "system.maintenance_status_on" : "system.maintenance_status_off";
            sender.sendMessage(new TextComponent(configManager.getMessage(key)));
            return;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("whitelist") && args[1].equalsIgnoreCase("list")) {
            StringBuilder list = new StringBuilder("§aWhitelisted players: §e");
            for (UUID uuid : configManager.getMaintenanceWhitelist()) {
                ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
                String name = (p != null) ? p.getName() : uuid.toString();
                list.append(name).append(", ");
            }
            if (list.lastIndexOf(", ") == list.length() - 2) {
                list.setLength(list.length() - 2);
            }
            sender.sendMessage(new TextComponent(list.toString()));
            return;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("on")) {
                maintenanceMode = true;
                configManager.getConfig().set("maintenance.enabled", true);
                configManager.saveConfig();
                sender.sendMessage(new TextComponent(configManager.getMessage("system.maintenance_enabled")));
                kickNonWhitelistedPlayers();
                return;
            }

            if (args[0].equalsIgnoreCase("off")) {
                maintenanceMode = false;
                configManager.getConfig().set("maintenance.enabled", false);
                configManager.saveConfig();
                sender.sendMessage(new TextComponent(configManager.getMessage("system.maintenance_disabled")));
                return;
            }

            if (args[0].equalsIgnoreCase("whitelist") && args.length >= 3) {
                if (args[1].equalsIgnoreCase("add") && args.length >= 3) {
                    ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(new TextComponent(configManager.getMessage("system.player_not_found", "player", args[2])));
                        return;
                    }
                    
                    UUID uuid = target.getUniqueId();
                    configManager.addToWhitelist(uuid);
                    sender.sendMessage(new TextComponent(configManager.getMessage("whitelist.player_added", "player", target.getName())));
                    return;
                }
                
                if (args[1].equalsIgnoreCase("remove") && args.length >= 3) {
                    ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(new TextComponent(configManager.getMessage("system.player_not_found", "player", args[2])));
                        return;
                    }
                    
                    UUID uuid = target.getUniqueId();
                    configManager.removeFromWhitelist(uuid);
                    sender.sendMessage(new TextComponent(configManager.getMessage("whitelist.player_removed", "player", target.getName())));
                    
                    // If maintenance is active, kick the player who was just removed from whitelist
                    if (maintenanceMode && target.isConnected()) {
                        target.disconnect(new TextComponent(
                                configManager.getMessage("join.maintenance_kick") + "\n" +
                                configManager.getMessage("join.maintenance_kick_info")
                        ));
                    }
                    return;
                }
            }
        }
        
        // Help message for invalid usage
        sender.sendMessage(new TextComponent("§eUsage: §f/maintenance [on|off]"));
        sender.sendMessage(new TextComponent("§eUsage: §f/maintenance whitelist add <player>"));
        sender.sendMessage(new TextComponent("§eUsage: §f/maintenance whitelist remove <player>"));
        sender.sendMessage(new TextComponent("§eUsage: §f/maintenance whitelist list"));
    }

    private void kickNonWhitelistedPlayers() {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.hasPermission("bungeesystem.maintenance.bypass")) {
                continue;
            }
            
            if (!configManager.isInWhitelist(player.getUniqueId())) {
                player.disconnect(new TextComponent(
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
