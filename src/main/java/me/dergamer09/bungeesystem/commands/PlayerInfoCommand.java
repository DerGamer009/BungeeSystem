package me.dergamer09.bungeesystem.commands;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlayerInfoCommand extends Command implements TabExecutor {

    private final BungeeSystem plugin;

    public PlayerInfoCommand(BungeeSystem plugin) {
        super("playerinfo", "bungeesystem.command.playerinfo", "pinfo", "whois");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                    "Usage: /playerinfo <player>"));
            return;
        }

        String targetName = args[0];
        
        // Check if API is enabled
        if (!plugin.getApiManager().isApiEnabled()) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                    "API integration is disabled. Cannot fetch player information."));
            return;
        }

        // Get player UUID
        ProxiedPlayer target = plugin.getProxy().getPlayer(targetName);
        String uuid;
        
        if (target != null) {
            uuid = target.getUniqueId().toString();
        } else {
            // Try to get UUID from database
            UUID playerUUID = plugin.getDatabaseManager().getUUIDFromName(targetName);
            if (playerUUID == null) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                        "Player not found: " + targetName));
                return;
            }
            uuid = playerUUID.toString();
        }

        // Fetch player info from API asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                String apiUrl = plugin.getApiManager().getApiUrl();
                String serverToken = plugin.getApiManager().getServerToken();
                
                URL url = new URL(apiUrl + "/auth/players/" + uuid);
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
                    
                    // Display player info
                    displayPlayerInfo(sender, targetName, response);
                } else if (responseCode == 404) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                            "Player information not found in the dashboard."));
                } else {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                            "Failed to fetch player information. HTTP " + responseCode));
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getErrorMessageColor() + 
                        "Error fetching player information: " + e.getMessage()));
            }
        });
    }
    
    private void displayPlayerInfo(CommandSender sender, String playerName, JSONObject playerData) {
        sender.sendMessage(new TextComponent(plugin.getPrefix() + plugin.getDefaultMessageColor() + 
                "Player Information for " + playerName + ":"));
        
        // Display basic info
        if (playerData.containsKey("name")) {
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Name: " + playerData.get("name")));
        }
        
        if (playerData.containsKey("uuid")) {
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  UUID: " + playerData.get("uuid")));
        }
        
        if (playerData.containsKey("first_join")) {
            long firstJoin = (Long) playerData.get("first_join");
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  First Join: " + 
                    new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(firstJoin))));
        }
        
        if (playerData.containsKey("last_seen")) {
            long lastSeen = (Long) playerData.get("last_seen");
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Last Seen: " + 
                    new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(lastSeen))));
        }
        
        // Display punishment counts
        if (playerData.containsKey("punishments")) {
            JSONObject punishments = (JSONObject) playerData.get("punishments");
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Punishments:"));
            
            if (punishments.containsKey("bans")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Bans: " + punishments.get("bans")));
            }
            
            if (punishments.containsKey("mutes")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Mutes: " + punishments.get("mutes")));
            }
            
            if (punishments.containsKey("kicks")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Kicks: " + punishments.get("kicks")));
            }
            
            if (punishments.containsKey("warns")) {
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Warnings: " + punishments.get("warns")));
            }
        }
        
        // Display current status
        if (playerData.containsKey("status")) {
            JSONObject status = (JSONObject) playerData.get("status");
            sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "  Status:"));
            
            if (status.containsKey("banned")) {
                boolean banned = (Boolean) status.get("banned");
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Banned: " + 
                        (banned ? plugin.getErrorMessageColor() + "Yes" : plugin.getSuccessMessageColor() + "No")));
            }
            
            if (status.containsKey("muted")) {
                boolean muted = (Boolean) status.get("muted");
                sender.sendMessage(new TextComponent(plugin.getDefaultMessageColor() + "    Muted: " + 
                        (muted ? plugin.getErrorMessageColor() + "Yes" : plugin.getSuccessMessageColor() + "No")));
            }
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest online players
            suggestions.addAll(plugin.getProxy().getPlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));
        }
        
        return suggestions;
    }
} 