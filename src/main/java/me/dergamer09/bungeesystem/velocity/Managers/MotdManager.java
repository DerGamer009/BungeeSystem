package me.dergamer09.bungeesystem.velocity.Managers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

/**
 * Handles MOTD messages for Velocity.
 */
public class MotdManager {
    private final ConfigManager configManager;
    private String normalMotd;
    private String maintenanceMotd;

    public MotdManager(ConfigManager configManager) {
        this.configManager = configManager;
        loadFromConfig();
    }

    /** Reload MOTD values from config. */
    public void loadFromConfig() {
        Configuration cfg = configManager.getConfig();
        normalMotd = ConfigManager.translateColorCodes(cfg.getString("motd.normal", "Welcome to the BungeeSystem Server!"));
        maintenanceMotd = ConfigManager.translateColorCodes(
                cfg.getString("motd.maintenance",
                        "&c\uD83D\uDEA7 Maintenance Mode - Server is currently under maintenance! \uD83D\uDEA7"));
    }

    /**
     * Get the MOTD depending on maintenance mode.
     *
     * @param maintenance whether maintenance mode is active
     * @return MOTD string
     */
    public String getMotd(boolean maintenance) {
        return maintenance ? maintenanceMotd : normalMotd;
    }
}
