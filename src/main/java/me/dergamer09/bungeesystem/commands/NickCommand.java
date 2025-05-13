package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class NickCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private static final Map<UUID, String> nicknames = new HashMap<>();
    private static final Map<String, UUID> nicknameToUUID = new HashMap<>();
    
    // Pattern to validate nicknames (alphanumeric, underscores, and colors)
    private static final Pattern VALID_NICKNAME = Pattern.compile("^[a-zA-Z0-9_&§]{3,16}$");

    public NickCommand() {
        super("nick", "bungeesystem.nick");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("general.player_only")));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        
        if (args.length < 1) {
            // Reset nickname if no args provided
            resetNickname(player);
            return;
        }
        
        String nickname = args[0];
        
        // Check for "off" or "reset" to remove nickname
        if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")) {
            resetNickname(player);
            return;
        }
        
        // Check if nickname is valid
        if (!VALID_NICKNAME.matcher(nickname).matches()) {
            player.sendMessage(new TextComponent(configManager.getMessage("nick.invalid_format")));
            return;
        }
        
        // Check if nickname is already taken
        if (isNicknameTaken(nickname, player.getUniqueId())) {
            player.sendMessage(new TextComponent(configManager.getMessage("nick.already_taken")));
            return;
        }
        
        // Store previous nickname for cleanup
        String previousNick = nicknames.get(player.getUniqueId());
        if (previousNick != null) {
            nicknameToUUID.remove(previousNick.toLowerCase());
        }
        
        // Translate color codes if player has permission
        if (player.hasPermission("bungeesystem.nick.color")) {
            nickname = ChatColor.translateAlternateColorCodes('&', nickname);
        }
        
        // Set the nickname
        nicknames.put(player.getUniqueId(), nickname);
        nicknameToUUID.put(nickname.toLowerCase(), player.getUniqueId());
        player.setDisplayName(nickname);
        
        player.sendMessage(new TextComponent(configManager.getMessage("nick.changed", "nickname", nickname)));
    }
    
    /**
     * Reset a player's nickname to their original name
     * 
     * @param player The player to reset the nickname for
     */
    private void resetNickname(ProxiedPlayer player) {
        String oldNick = nicknames.remove(player.getUniqueId());
        if (oldNick != null) {
            nicknameToUUID.remove(oldNick.toLowerCase());
        }
        
        player.setDisplayName(player.getName());
        player.sendMessage(new TextComponent(configManager.getMessage("nick.reset")));
    }
    
    /**
     * Check if a nickname is already taken by another player
     * 
     * @param nickname The nickname to check
     * @param playerUUID The UUID of the player attempting to use the nickname
     * @return true if the nickname is taken by another player, false otherwise
     */
    private boolean isNicknameTaken(String nickname, UUID playerUUID) {
        nickname = ChatColor.translateAlternateColorCodes('&', nickname).toLowerCase();
        
        UUID existingPlayer = nicknameToUUID.get(nickname);
        return existingPlayer != null && !existingPlayer.equals(playerUUID);
    }
    
    /**
     * Get a player's original name from their nickname
     * 
     * @param nickname The nickname to lookup
     * @return The UUID of the player with that nickname, or null if not found
     */
    public static UUID getPlayerFromNickname(String nickname) {
        return nicknameToUUID.get(nickname.toLowerCase());
    }
    
    /**
     * Get a player's nickname
     * 
     * @param uuid The UUID of the player
     * @return The player's nickname, or null if they don't have one
     */
    public static String getNickname(UUID uuid) {
        return nicknames.get(uuid);
    }
    
    /**
     * Check if a player has a nickname
     * 
     * @param uuid The UUID of the player
     * @return true if the player has a nickname, false otherwise
     */
    public static boolean hasNickname(UUID uuid) {
        return nicknames.containsKey(uuid);
    }
} 