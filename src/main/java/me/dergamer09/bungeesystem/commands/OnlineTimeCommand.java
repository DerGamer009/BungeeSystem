package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class OnlineTimeCommand extends Command {

    public OnlineTimeCommand() {
        super("onlinetime", null, "otime");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        UUID targetUUID;
        String name;

        if (args.length == 0) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage("§cOnly players can use this command without arguments.");
                return;
            }
            targetUUID = ((ProxiedPlayer) sender).getUniqueId();
            name = ((ProxiedPlayer) sender).getName();
        } else {
            ProxiedPlayer target = BungeeSystem.getInstance().getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cThat player is not online.");
                return;
            }
            targetUUID = target.getUniqueId();
            name = target.getName();
        }

        try (PreparedStatement ps = BungeeSystem.getInstance().getDatabaseManager().getConnection().prepareStatement(
                "SELECT total_time FROM online_time WHERE player_uuid = ?")) {
            ps.setString(1, targetUUID.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long millis = rs.getLong("total_time");
                    long seconds = millis / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;

                    seconds %= 60;
                    minutes %= 60;

                    sender.sendMessage("§e" + name + " §7has been online for §a" +
                            hours + "h " + minutes + "m " + seconds + "s§7.");
                } else {
                    sender.sendMessage("§cNo data found for that player.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage("§cAn error occurred while retrieving data.");
        }
    }
}
