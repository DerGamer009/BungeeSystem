package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.PunishmentManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class UnmuteCommand extends Command implements TabExecutor {

    private final BungeeSystem plugin;
    private final PunishmentManager punishmentManager;

    public UnmuteCommand(BungeeSystem plugin) {
        super("unmute", "bungeesystem.command.unmute");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                    "Usage: /unmute <player> [reason]"));
            return;
        }

        String targetName = args[0];
        String reason = "No reason provided";
        
        if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(" ");
            }
            reason = reasonBuilder.toString().trim();
        }
        
        // Execute the unmute
        boolean success = punishmentManager.unmutePlayer(sender, targetName, reason);
        
        if (!success) {
            // The punishmentManager will send appropriate error messages
        }
    }
    
    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Tab complete player names
            String search = args[0].toLowerCase();
            return ProxyServer.getInstance().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(search))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
} 