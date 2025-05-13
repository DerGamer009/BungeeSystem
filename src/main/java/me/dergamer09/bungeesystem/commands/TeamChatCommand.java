package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ChatManager;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeamChatCommand extends Command implements TabExecutor {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;
    private final ChatManager chatManager;
    
    private static final List<String> CHANNEL_NAMES = Arrays.asList(
            ChatManager.ADMIN_CHANNEL,
            ChatManager.SUPPORT_CHANNEL,
            ChatManager.DEV_CHANNEL,
            ChatManager.MOD_CHANNEL,
            ChatManager.TEAM_CHANNEL
    );

    public TeamChatCommand() {
        super("teamchat", "bungeesystem.teamchat.use", "tc");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
        this.chatManager = plugin.getChatManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("bungeesystem.teamchat.use")) {
            sender.sendMessage(new TextComponent(configManager.getMessage("general.no_permission")));
            return;
        }
        
        // If no arguments provided, show usage
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }
        
        // Check for channel selection
        if (args[0].equalsIgnoreCase("channel") || args[0].equalsIgnoreCase("chan")) {
            handleChannelCommand(sender, args);
            return;
        }
        
        // Determine the message and channel to use
        String channel;
        String message;
        
        // Check if first arg is a channel name
        if (CHANNEL_NAMES.contains(args[0].toLowerCase()) && args.length > 1) {
            // If sender has permission for that channel, use it
            if (chatManager.hasChannelPermission(sender, args[0].toLowerCase())) {
                channel = args[0].toLowerCase();
                message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            } else {
                sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.channel_no_permission")));
                return;
            }
        } else {
            // Get player's current channel or use default
            if (sender instanceof ProxiedPlayer) {
                channel = chatManager.getPlayerChannel((ProxiedPlayer) sender);
            } else {
                channel = ChatManager.TEAM_CHANNEL; // Default for console
            }
            
            // Message is all args
            message = String.join(" ", args);
        }
        
        // Send the message
        chatManager.sendTeamChatMessage(sender, channel, message);
    }
    
    /**
     * Handle channel-related commands
     * 
     * @param sender The command sender
     * @param args Command arguments
     */
    private void handleChannelCommand(CommandSender sender, String[] args) {
        // Require a player
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.player_only")));
            return;
        }
        
        ProxiedPlayer player = (ProxiedPlayer) sender;
        
        // If just "/tc channel", show current channel
        if (args.length == 1) {
            String currentChannel = chatManager.getPlayerChannel(player);
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.current_channel", 
                    "channel", currentChannel)));
            return;
        }
        
        // Get requested channel
        String requestedChannel = args[1].toLowerCase();
        
        // Check if channel is valid
        if (!CHANNEL_NAMES.contains(requestedChannel)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.invalid_channel")));
            return;
        }
        
        // Try to set the channel
        if (chatManager.setPlayerChannel(player, requestedChannel)) {
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.channel_set", 
                    "channel", requestedChannel)));
        } else {
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.channel_no_permission")));
        }
    }
    
    /**
     * Send usage information to the sender
     * 
     * @param sender The command sender
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.usage_header")));
        sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.usage_message")));
        
        // Only show channel commands if player has more than the default permission
        if (sender.hasPermission("bungeesystem.teamchat.mod") || 
            sender.hasPermission("bungeesystem.teamchat.admin") ||
            sender.hasPermission("bungeesystem.teamchat.dev") ||
            sender.hasPermission("bungeesystem.teamchat.support")) {
            
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.usage_channel")));
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.usage_direct")));
            
            // Show available channels
            StringBuilder channels = new StringBuilder();
            for (String channel : CHANNEL_NAMES) {
                if (chatManager.hasChannelPermission(sender, channel)) {
                    if (channels.length() > 0) {
                        channels.append(", ");
                    }
                    channels.append(channel);
                }
            }
            
            sender.sendMessage(new TextComponent(configManager.getMessage("teamchat.available_channels", 
                    "channels", channels.toString())));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            
            // Add "channel" as a suggestion
            if ("channel".startsWith(args[0].toLowerCase()) || "chan".startsWith(args[0].toLowerCase())) {
                completions.add("channel");
            }
            
            // Add channels the sender has permission for
            for (String channel : CHANNEL_NAMES) {
                if (channel.startsWith(args[0].toLowerCase()) && chatManager.hasChannelPermission(sender, channel)) {
                    completions.add(channel);
                }
            }
            
            return completions;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("channel") || args[0].equalsIgnoreCase("chan"))) {
            // Show channel suggestions for "/tc channel <tab>"
            List<String> completions = new ArrayList<>();
            
            for (String channel : CHANNEL_NAMES) {
                if (channel.startsWith(args[1].toLowerCase()) && chatManager.hasChannelPermission(sender, channel)) {
                    completions.add(channel);
                }
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}
