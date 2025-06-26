package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Simple nickname command for Velocity.
 */
public class NickCommand implements SimpleCommand {
    private final ConfigManager configManager;
    private static final Map<UUID, String> nicknames = new HashMap<>();
    private static final Map<String, UUID> nicknameToUUID = new HashMap<>();
    private static final Pattern VALID_NICKNAME = Pattern.compile("^[a-zA-Z0-9_&§]{3,16}$");

    public NickCommand(VelocitySystem plugin) {
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text(configManager.getMessage("general.player_only")));
            return;
        }
        Player player = (Player) source;

        if (args.length < 1) {
            resetNickname(player);
            return;
        }
        String nickname = args[0];

        if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")) {
            resetNickname(player);
            return;
        }

        if (!VALID_NICKNAME.matcher(nickname).matches()) {
            player.sendMessage(Component.text(configManager.getMessage("nick.invalid_format")));
            return;
        }

        if (isNicknameTaken(nickname, player.getUniqueId())) {
            player.sendMessage(Component.text(configManager.getMessage("nick.already_taken")));
            return;
        }

        String previousNick = nicknames.get(player.getUniqueId());
        if (previousNick != null) {
            nicknameToUUID.remove(previousNick.toLowerCase());
        }

        if (player.hasPermission("bungeesystem.nick.color")) {
            nickname = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', nickname);
        }

        nicknames.put(player.getUniqueId(), nickname);
        nicknameToUUID.put(nickname.toLowerCase(), player.getUniqueId());
        player.sendMessage(Component.text(configManager.getMessage("nick.changed", "nickname", nickname)));
    }

    private void resetNickname(Player player) {
        String oldNick = nicknames.remove(player.getUniqueId());
        if (oldNick != null) {
            nicknameToUUID.remove(oldNick.toLowerCase());
        }
        player.sendMessage(Component.text(configManager.getMessage("nick.reset")));
    }

    private boolean isNicknameTaken(String nickname, UUID uuid) {
        nickname = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', nickname).toLowerCase();
        UUID existing = nicknameToUUID.get(nickname);
        return existing != null && !existing.equals(uuid);
    }

    public static UUID getPlayerFromNickname(String nickname) {
        return nicknameToUUID.get(nickname.toLowerCase());
    }

    public static String getNickname(UUID uuid) {
        return nicknames.get(uuid);
    }

    public static boolean hasNickname(UUID uuid) {
        return nicknames.containsKey(uuid);
    }
}
