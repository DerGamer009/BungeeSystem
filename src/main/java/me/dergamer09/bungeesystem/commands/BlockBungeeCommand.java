package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;

public class BlockBungeeCommand extends Command {

    private final BungeeSystem plugin;

    public BlockBungeeCommand(BungeeSystem plugin) {
        super("bungee", "bungeesystem.bungee");
        this.plugin = plugin; // Initialisierung der plugin-Variable
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = plugin.getPrefix();
        if (sender.hasPermission("bungeesystem.allow.bungee")) {
            sender.sendMessage( plugin.getPrefix() + plugin.getSuccessMessageColor() + "Du hast die Berechtigung, diesen Befehl auszuführen.");

            // Abfrage der BungeeCord-Version
            String version = ProxyServer.getInstance().getVersion();
            sender.sendMessage(plugin.getPrefix() + plugin.getSuccessMessageColor() + "BungeeCord Version: " + plugin.getUpdateMessageColor() + version);

            // Hier könntest du weitere Befehlsausführungen hinzufügen.
        } else if (sender.hasPermission("bungeesystem.notallowd.bungee")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getErrorMessageColor() + "Entschuldigung, dieser Befehl ist nicht erlaubt.");
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getErrorMessageColor() + "Du hast keine Berechtigung, diesen Befehl auszuführen.");
        }
    }
}
