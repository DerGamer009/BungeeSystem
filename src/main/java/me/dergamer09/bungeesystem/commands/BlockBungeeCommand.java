package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class BlockBungeeCommand extends Command {

    private final String prefix = ChatColor.DARK_GRAY + "| " + ChatColor.RED + "ᴍɪɴᴇᴄᴏꜱɪᴀ " + ChatColor.GRAY + "» ";

    public BlockBungeeCommand() {
        super("bungee");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("bungeesystem.allow.bungee")) {
            sender.sendMessage(prefix + ChatColor.GREEN + "Du hast die Berechtigung, diesen Befehl auszuführen.");
            // Hier könntest du die tatsächliche Befehlsausführung hinzufügen.
        } else if (sender.hasPermission("bungeesystem.notallowd.bungee")) {
            sender.sendMessage(prefix + ChatColor.GOLD + "Entschuldigung, dieser Befehl ist nicht erlaubt.");
        } else {
            sender.sendMessage(prefix + ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl auszuführen.");
        }
    }
}