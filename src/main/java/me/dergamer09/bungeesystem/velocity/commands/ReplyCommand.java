package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import net.kyori.adventure.text.Component;

import java.util.*;

public class ReplyCommand implements SimpleCommand {
    private final VelocitySystem plugin;
    private final ConfigManager configManager;
    public ReplyCommand(VelocitySystem plugin) {
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
        Player player = (Player) source;
        Map<UUID, UUID> lastMessageMap = MSGCommand.getLastMessageMap();
        Map<UUID, Set<UUID>> ignoredPlayers = MSGCommand.getIgnoredPlayers();
        if (!lastMessageMap.containsKey(player.getUniqueId())) {
            player.sendMessage(configManager.getMessageComponent("reply.no_one"));
            return;
        }
        if (args.length < 1) {
            player.sendMessage(configManager.getMessageComponent("reply.usage"));
            return;
        }
        UUID targetUUID = lastMessageMap.get(player.getUniqueId());
        Optional<Player> targetOpt = plugin.getServer().getPlayer(targetUUID);
        if (targetOpt.isEmpty()) {
            player.sendMessage(configManager.getMessageComponent("reply.not_online"));
            return;
        }
        Player target = targetOpt.get();
        Set<UUID> ignored = ignoredPlayers.getOrDefault(target.getUniqueId(), new HashSet<>());
        if (ignored.contains(player.getUniqueId())) {
            player.sendMessage(configManager.getMessageComponent("msg.ignored_by"));
            return;
        }
        String message = String.join(" ", args);
        player.sendMessage(Component.text(configManager.getMessage("msg.to_format", "target", target.getUsername(), "message", message)));
        target.sendMessage(Component.text(configManager.getMessage("msg.from_format", "sender", player.getUsername(), "message", message)));
        lastMessageMap.put(player.getUniqueId(), target.getUniqueId());
        lastMessageMap.put(target.getUniqueId(), player.getUniqueId());
    }
}
