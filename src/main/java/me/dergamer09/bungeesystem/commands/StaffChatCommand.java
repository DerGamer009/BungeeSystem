package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ChatManager;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class StaffChatCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private final ChatManager chatManager;

    public StaffChatCommand() {
        super("staffchat", "bungeesystem.staffchat.use", "sc", "staff");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
        this.chatManager = plugin.getChatManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check if player has permission
        if (!sender.hasPermission("bungeesystem.staffchat.use")) {
            sender.sendMessage(new TextComponent(configManager.getMessage("general.no_permission")));
            return;
        }
        
        // If no message is provided, show usage
        if (args.length == 0) {
            sender.sendMessage(new TextComponent(configManager.getMessage("staffchat.usage")));
            return;
        }
        
        // Join the message arguments
        String message = String.join(" ", args);
        
        // Send message via the ChatManager
        chatManager.sendTeamChatMessage(sender, ChatManager.TEAM_CHANNEL, message);
    }
} 