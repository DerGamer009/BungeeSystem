package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.PunishmentManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class UnbanCommand extends Command {

    private final BungeeSystem plugin;
    private final PunishmentManager punishmentManager;

    public UnbanCommand(BungeeSystem plugin) {
        super("unban", "bungeesystem.command.unban", "pardon");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                    "Usage: /unban <player> [reason]"));
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
        
        // Execute the unban
        boolean success = punishmentManager.unbanPlayer(sender, targetName, reason);
        
        if (!success) {
            // The punishmentManager will send appropriate error messages
        }
    }
} 