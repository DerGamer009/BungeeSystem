package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import net.kyori.adventure.text.Component;

import java.util.*;

public class IgnoreCommand implements SimpleCommand {
    private final VelocitySystem plugin;
    private final ConfigManager configManager;
    public IgnoreCommand(VelocitySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source instanceof Player)) {
            source.sendMessage(configManager.getMessageComponent("lobby.player_only"));
            return;
        }
        if (args.length != 1) {
            source.sendMessage(configManager.getMessageComponent("ignore.usage"));
            return;
        }
        Player player = (Player) source;
        Optional<Player> targetOpt = plugin.getServer().getPlayer(args[0]);
        if (targetOpt.isEmpty()) {
            player.sendMessage(configManager.getMessageComponent("ignore.not_online"));
            return;
        }
        Player target = targetOpt.get();
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(configManager.getMessageComponent("ignore.cant_ignore_self"));
            return;
        }
        Map<UUID, Set<UUID>> ignoredPlayers = MSGCommand.getIgnoredPlayers();
        Set<UUID> ignored = ignoredPlayers.getOrDefault(player.getUniqueId(), new HashSet<>());
        if (ignored.contains(target.getUniqueId())) {
            ignored.remove(target.getUniqueId());
            player.sendMessage(configManager.getMessageComponent("ignore.unignored", "player", target.getUsername()));
        } else {
            ignored.add(target.getUniqueId());
            player.sendMessage(configManager.getMessageComponent("ignore.ignored", "player", target.getUsername()));
        }
        ignoredPlayers.put(player.getUniqueId(), ignored);
    }
}
