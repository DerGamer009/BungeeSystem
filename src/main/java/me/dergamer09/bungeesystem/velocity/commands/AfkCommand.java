package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Basic AFK command implementation for Velocity.
 */
public class AfkCommand implements SimpleCommand {
    private static final Map<UUID, Boolean> afkPlayers = new HashMap<>();
    private final ProxyServer server;
    private final ConfigManager configManager;

    public AfkCommand(VelocitySystem plugin) {
        this.server = plugin.getServer();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text(configManager.getMessage("general.player_only")));
            return;
        }

        Player player = (Player) source;
        UUID uuid = player.getUniqueId();
        boolean isAfk = !isAfk(uuid);
        afkPlayers.put(uuid, isAfk);

        if (isAfk) {
            server.sendMessage(Component.text(
                    configManager.getMessage("afk.player_now_afk", "player", player.getUsername())
            ));
        } else {
            server.sendMessage(Component.text(
                    configManager.getMessage("afk.player_no_longer_afk", "player", player.getUsername())
            ));
        }
    }

    /**
     * Check if a player is currently AFK.
     */
    public static boolean isAfk(UUID uuid) {
        return afkPlayers.getOrDefault(uuid, false);
    }

    /**
     * Directly set a player's AFK state.
     */
    public static void setAfk(UUID uuid, boolean afk) {
        afkPlayers.put(uuid, afk);
    }

    /**
     * Clear the AFK state on disconnect.
     */
    public static void clearAfk(UUID uuid) {
        afkPlayers.remove(uuid);
    }
}
