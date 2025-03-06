package me.dergamer09.bungeesystem.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;


public class MaintenanceCommand extends Command {

    private static boolean maintenancemode = false;

    public MaintenanceCommand() {
        super("maintenance", "bungeesystem.maintenance");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        maintenancemode = !maintenancemode;
        String status = maintenancemode ? "enabled" : "disabled";
        sender.sendMessage(ChatColor.YELLOW + "maintenance mode has been set to " + status + ".");
    }

    public static boolean isMaintenanceMode() {
        return maintenancemode;
    }

}
