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

public class ReplyCommand extends Command {

    public ReplyCommand() {
        super("reply", "", "r");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("This command can only be used by players."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!BungeeSystem.lastMessageMap.containsKey(player.getUniqueId())) {
            player.sendMessage(new TextComponent("§cThere is no one to reply to."));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(new TextComponent("§cUsage: /reply <message>"));
            return;
        }


        UUID targetUUID = BungeeSystem.lastMessageMap.get(player.getUniqueId());
        ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(targetUUID);

        if (targetPlayer == null) {
            player.sendMessage(new TextComponent("§cThat player is not online."));
            return;
        }

        Set<UUID> ignored = BungeeSystem.ignoredPlayers.getOrDefault(targetPlayer.getUniqueId(), new HashSet<>());
        if (ignored.contains(player.getUniqueId())) {
            player.sendMessage(new TextComponent("§cThat player is ignoring you."));
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }
        String message = messageBuilder.toString().trim();

        player.sendMessage(new TextComponent("§7[§bYou §7→ §a" + targetPlayer.getName() + "§7] §f" + message));
        targetPlayer.sendMessage(new TextComponent("§7[§a" + player.getName() + " §7→ §bYou§7] §f" + message));

        // Update last message mapping
        BungeeSystem.lastMessageMap.put(player.getUniqueId(), targetPlayer.getUniqueId());
        BungeeSystem.lastMessageMap.put(targetPlayer.getUniqueId(), player.getUniqueId());
    }
}
