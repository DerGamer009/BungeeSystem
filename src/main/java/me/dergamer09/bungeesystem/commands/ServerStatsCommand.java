package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ServerStatsCommand extends Command implements TabExecutor {

    private final BungeeSystem plugin;

    public ServerStatsCommand(BungeeSystem plugin) {
        super("serverstats", "bungeesystem.command.serverstats", "stats");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check if API is enabled
        if (!plugin.getApiManager().isApiEnabled()) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                    "API integration is disabled. Cannot fetch server statistics."));
            return;
        }

        String serverId = plugin.getApiManager().getServerId();
        if (serverId == null || serverId.isEmpty()) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                    "Server not connected to API. Please check your configuration."));
            return;
        }

        // Fetch server stats from API asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                String apiUrl = plugin.getApiManager().getApiUrl();
                String serverToken = plugin.getApiManager().getServerToken();
                
                URL url = new URL(apiUrl + "/auth/servers/" + serverId + "/status");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-API-Key", serverToken);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // Parse response
                    JSONParser parser = new JSONParser();
                    JSONObject response = (JSONObject) parser.parse(new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                    
                    // Display server stats
                    displayServerStats(sender, response);
                } else if (responseCode == 404) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                            "Server statistics not found in the dashboard."));
                } else {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                            "Failed to fetch server statistics. HTTP " + responseCode));
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                        "Error fetching server statistics: " + e.getMessage()));
            }
        });
    }
    
    private void displayServerStats(CommandSender sender, JSONObject statsData) {
        sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                "Server Statistics:"));
        
        // Display basic server info
        if (statsData.containsKey("server_name")) {
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Server: " + statsData.get("server_name")));
        }
        
        if (statsData.containsKey("server_id")) {
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  ID: " + statsData.get("server_id")));
        }
        
        // Display player statistics
        if (statsData.containsKey("players")) {
            JSONObject players = (JSONObject) statsData.get("players");
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Players:"));
            
            if (players.containsKey("online")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Online: " + players.get("online")));
            }
            
            if (players.containsKey("max")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Max: " + players.get("max")));
            }
            
            if (players.containsKey("total_unique")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Total Unique: " + players.get("total_unique")));
            }
        }
        
        // Display punishment statistics
        if (statsData.containsKey("punishments")) {
            JSONObject punishments = (JSONObject) statsData.get("punishments");
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Punishments:"));
            
            if (punishments.containsKey("total_bans")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Total Bans: " + punishments.get("total_bans")));
            }
            
            if (punishments.containsKey("active_bans")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Active Bans: " + punishments.get("active_bans")));
            }
            
            if (punishments.containsKey("total_mutes")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Total Mutes: " + punishments.get("total_mutes")));
            }
            
            if (punishments.containsKey("active_mutes")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Active Mutes: " + punishments.get("active_mutes")));
            }
            
            if (punishments.containsKey("total_kicks")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Total Kicks: " + punishments.get("total_kicks")));
            }
            
            if (punishments.containsKey("total_warns")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Total Warnings: " + punishments.get("total_warns")));
            }
        }
        
        // Display report statistics
        if (statsData.containsKey("reports")) {
            JSONObject reports = (JSONObject) statsData.get("reports");
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Reports:"));
            
            if (reports.containsKey("total")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Total: " + reports.get("total")));
            }
            
            if (reports.containsKey("open")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Open: " + reports.get("open")));
            }
            
            if (reports.containsKey("handled")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Handled: " + reports.get("handled")));
            }
        }
        
        // Display uptime
        if (statsData.containsKey("uptime")) {
            long uptime = (Long) statsData.get("uptime");
            long hours = uptime / 3600000; // Convert milliseconds to hours
            long minutes = (uptime % 3600000) / 60000; // Convert remaining milliseconds to minutes
            
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Uptime: " + 
                    hours + "h " + minutes + "m"));
        }
        
        // Display last update
        if (statsData.containsKey("last_update")) {
            long lastUpdate = (Long) statsData.get("last_update");
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Last Update: " + 
                    new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(lastUpdate))));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        // No tab completion needed for this command
        return new java.util.ArrayList<>();
    }
} 