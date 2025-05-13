package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.Managers.ConfigManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RestartCommand extends Command {

    private final BungeeSystem plugin;
    private final ConfigManager configManager;

    public RestartCommand() {
        super("restart", "bungeesystem.admin.restart");
        this.plugin = BungeeSystem.getInstance();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent("§eUsage: §f/restart <server>"));
            return;
        }

        String serverName = args[0];
        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(serverName);
        
        if (serverInfo == null) {
            sender.sendMessage(new TextComponent(configManager.getMessage("system.server_not_found", "server", serverName)));
            return;
        }

        try {
            restartServer(serverName, sender);
        } catch (Exception e) {
            sender.sendMessage(new TextComponent(
                    configManager.getMessage("system.server_restart_failed", 
                            "server", serverName,
                            "error", e.getMessage())
            ));
            plugin.getLogger().severe("Failed to restart server " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void restartServer(String serverName, CommandSender sender) throws IOException, InterruptedException {
        boolean useScreenCommand = configManager.getConfig().getBoolean("restart.enable_screen_command", true);
        
        if (useScreenCommand) {
            String screenCommandPrefix = configManager.getConfig().getString("restart.screen_command_prefix", "screen -S mc_%server% -p 0 -X stuff");
            String restartCommand = configManager.getConfig().getString("restart.restart_command", "stop\n");
            
            // Replace %server% with the actual server name
            screenCommandPrefix = screenCommandPrefix.replace("%server%", serverName);
            
            String fullCommand = screenCommandPrefix + " \"" + restartCommand + "\"";
            
            // Execute the command in the OS
            ProcessBuilder pb = new ProcessBuilder();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", fullCommand);
            } else {
                pb.command("bash", "-c", fullCommand);
            }
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                sender.sendMessage(new TextComponent(configManager.getMessage("system.server_restart_success", "server", serverName)));
            } else {
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                }
                
                throw new IOException("Command exited with code " + exitCode + ": " + errorOutput.toString());
            }
        } else {
            // Fallback to sending a plugin message to the server for restart
            // Implementation depends on if you have a plugin on the server side to handle this
            sender.sendMessage(new TextComponent("§cRestarting via remote call is not implemented yet. " +
                    "Please enable screen commands in the config."));
        }
    }
} 