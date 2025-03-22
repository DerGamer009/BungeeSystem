package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MSGCommand extends Command {

    public MSGCommand() {
        super("msg", "", "pm", "tell");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(new TextComponent("Usage: /msg <player> <message>"));
            return;
        }

        ProxiedPlayer senderPlayer = (ProxiedPlayer) sender;
        ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(args[0]);

        if (targetPlayer == null) {
            sender.sendMessage(new TextComponent("That player is not online!"));
            return;
        }

        Set<UUID> ignored = BungeeSystem.ignoredPlayers.getOrDefault(targetPlayer.getUniqueId(), new HashSet<>());
        if (ignored.contains(senderPlayer.getUniqueId())) {
            senderPlayer.sendMessage(new TextComponent("§cThat player is ignoring you."));
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        senderPlayer.sendMessage(new TextComponent("§7[§bYou §7→ §a" + targetPlayer.getName() + "§7] §f" + message));
        targetPlayer.sendMessage(new TextComponent("§7[§a" + senderPlayer.getName() + "§7→ §bYou§7]" + message));

        // Save the last message pair for both players
        BungeeSystem.lastMessageMap.put(senderPlayer.getUniqueId(), targetPlayer.getUniqueId());
        BungeeSystem.lastMessageMap.put(targetPlayer.getUniqueId(), senderPlayer.getUniqueId());
    }

}
