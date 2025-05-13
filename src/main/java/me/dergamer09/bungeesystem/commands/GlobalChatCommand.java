package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ChatManager;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class GlobalChatCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private final ChatManager chatManager;

    public GlobalChatCommand() {
        super("globalchat", "bungeesystem.globalchat.use", "g", "global");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
        this.chatManager = plugin.getChatManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("general.player_only")));
            return;
        }
        
        ProxiedPlayer player = (ProxiedPlayer) sender;
        
        // Check if player has permission
        if (!player.hasPermission("bungeesystem.globalchat.use")) {
            player.sendMessage(new TextComponent(configManager.getMessage("general.no_permission")));
            return;
        }
        
        // Toggle mode if no arguments provided
        if (args.length == 0) {
            boolean enabled = chatManager.toggleGlobalChat(player);
            if (enabled) {
                player.sendMessage(new TextComponent(configManager.getMessage("globalchat.enabled")));
            } else {
                player.sendMessage(new TextComponent(configManager.getMessage("globalchat.disabled")));
            }
            return;
        }
        
        // Send the message
        String message = String.join(" ", args);
        chatManager.sendGlobalChatMessage(player, message);
    }
} 