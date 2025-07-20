package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.config.Configuration;

/**
 * Handles MOTD messages for BungeeCord.
 */
public class MotdManager {
    private final BungeeSystem plugin;
    private String normalMotd;
    private String maintenanceMotd;

    public MotdManager(BungeeSystem plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    /** Reload MOTD values from config. */
    public void loadFromConfig() {
        Configuration cfg = plugin.getConfig();
        MiniMessage mm = MiniMessage.miniMessage();
        normalMotd = LegacyComponentSerializer.legacySection().serialize(
                mm.deserialize(cfg.getString("motd.normal", "Welcome to the BungeeSystem Server!")));
        maintenanceMotd = LegacyComponentSerializer.legacySection().serialize(
                mm.deserialize(cfg.getString("motd.maintenance",
                        "&c\uD83D\uDEA7 Maintenance Mode - Server is currently under maintenance! \uD83D\uDEA7")));
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
