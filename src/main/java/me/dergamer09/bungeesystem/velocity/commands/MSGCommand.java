package me.dergamer09.bungeesystem.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import net.kyori.adventure.text.Component;

import java.util.*;

public class MSGCommand implements SimpleCommand {
    private static final Map<UUID, UUID> lastMessageMap = new HashMap<>();
    private static final Map<UUID, Set<UUID>> ignoredPlayers = new HashMap<>();
    private final ConfigManager configManager;
    private final VelocitySystem plugin;

    public MSGCommand(VelocitySystem plugin) {
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
        if (args.length < 2) {
            source.sendMessage(configManager.getMessageComponent("msg.usage"));
            return;
        }
        Player sender = (Player) source;
        Optional<Player> targetOpt = plugin.getServer().getPlayer(args[0]);
        if (targetOpt.isEmpty()) {
            sender.sendMessage(configManager.getMessageComponent("msg.not_online"));
            return;
        }
        Player target = targetOpt.get();
        Set<UUID> ignored = ignoredPlayers.getOrDefault(target.getUniqueId(), new HashSet<>());
        if (ignored.contains(sender.getUniqueId())) {
            sender.sendMessage(configManager.getMessageComponent("msg.ignored_by"));
            return;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sender.sendMessage(Component.text(configManager.getMessage("msg.to_format", "target", target.getUsername(), "message", message)));
        target.sendMessage(Component.text(configManager.getMessage("msg.from_format", "sender", sender.getUsername(), "message", message)));
        lastMessageMap.put(sender.getUniqueId(), target.getUniqueId());
        lastMessageMap.put(target.getUniqueId(), sender.getUniqueId());
    }
    public static Map<UUID, UUID> getLastMessageMap() { return lastMessageMap; }
    public static Map<UUID, Set<UUID>> getIgnoredPlayers() { return ignoredPlayers; }
}
