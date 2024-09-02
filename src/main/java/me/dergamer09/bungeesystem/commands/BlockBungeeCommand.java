package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;

public class BlockBungeeCommand extends Command {

    private String prefix;

    public BlockBungeeCommand() {
        super("bungee");
        // Laden des Prefix aus der config.yml
        prefix = ChatColor.translateAlternateColorCodes('&', BungeeSystem.getInstance().getConfig().getString("prefix"));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("bungeesystem.allow.bungee")) {
            sender.sendMessage(prefix + ChatColor.GREEN + "Du hast die Berechtigung, diesen Befehl auszuführen.");

            // Abfrage der BungeeCord-Version
            String version = ProxyServer.getInstance().getVersion();
            sender.sendMessage(prefix + ChatColor.AQUA + "BungeeCord Version: " + ChatColor.WHITE + version);

            // Hier könntest du weitere Befehlsausführungen hinzufügen.
        } else if (sender.hasPermission("bungeesystem.notallowd.bungee")) {
            sender.sendMessage(prefix + ChatColor.GOLD + "Entschuldigung, dieser Befehl ist nicht erlaubt.");
        } else {
            sender.sendMessage(prefix + ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl auszuführen.");
        }
    }
}
