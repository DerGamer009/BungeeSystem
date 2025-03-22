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

public class IgnoreCommand extends Command {

    public IgnoreCommand() {
        super("ignore");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("This command can only be used by players."));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponent("§cUsage: /ignore <player>"));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(new TextComponent("§cThat player is not online."));
            return;
        }

        if (target.equals(player)) {
            sender.sendMessage(new TextComponent("§cYou can't ignore yourself."));
            return;
        }

        Set<UUID> ignored = BungeeSystem.ignoredPlayers.getOrDefault(player.getUniqueId(), new HashSet<>());

        if (ignored.contains(target.getUniqueId())) {
            ignored.remove(target.getUniqueId());
            sender.sendMessage(new TextComponent("§aYou have unignored " + target.getName() + "."));
        } else {
            ignored.add(target.getUniqueId());
            sender.sendMessage(new TextComponent("§cYou are now ignoring " + target.getName() + "."));
        }

        BungeeSystem.ignoredPlayers.put(player.getUniqueId(), ignored);
    }
}
