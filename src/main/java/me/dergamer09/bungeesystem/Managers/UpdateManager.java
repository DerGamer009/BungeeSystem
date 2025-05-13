package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Manages update checking for the BungeeSystem plugin
 */
public class UpdateManager {

    private final BungeeSystem plugin;
    private final String currentVersion;
    private final String spigotResourceId = "119339"; // ID of your resource on SpigotMC
    
    public UpdateManager(BungeeSystem plugin, String currentVersion) {
        this.plugin = plugin;
        this.currentVersion = currentVersion;
    }
    
    /**
     * Checks for updates from SpigotMC
     */
    public void checkForUpdates() {
        plugin.getLogger().info("Checking for updates...");
        
        try {
            // Build URL for SpigotMC API
            URL url = new URL("https://api.spigotmc.org/simple/0.2/index.php?action=getResource&id=" + spigotResourceId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                connection.disconnect();

                // Parse the JSON response
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(content.toString());
                String latestVersion = (String) jsonResponse.get("current_version");

                // Compare versions
                if (latestVersion != null && isNewerVersion(latestVersion, currentVersion)) {
                    plugin.getLogger().warning(ChatColor.YELLOW + "A new update is available: " + latestVersion + " (Current Version: " + currentVersion + ")");
                } else {
                    plugin.getLogger().info(ChatColor.GREEN + "Your plugin is up to date.");
                }
            } else {
                plugin.getLogger().severe(ChatColor.RED + "Error retrieving version information from SpigotMC. HTTP Error Code: " + responseCode);
            }
        } catch (Exception e) {
            plugin.getLogger().severe(ChatColor.RED + "Error checking for updates via SpigotMC: " + e.getMessage());
        }
    }
    
    /**
     * Checks if two versions are the same ignoring the -SNAPSHOT suffix
     * 
     * @param otherVersion Version to compare with current version
     * @return true if versions match (ignoring -SNAPSHOT)
     */
    public boolean isSameSnapshot(String otherVersion) {
        return currentVersion.replaceAll("-SNAPSHOT", "").equals(otherVersion.replaceAll("-SNAPSHOT", ""));
    }
    
    /**
     * Compare version strings using semantic versioning rules
     * 
     * @param newVersion The new version to check
     * @param currentVersion The current version
     * @return true if newVersion is actually newer than currentVersion
     */
    public boolean isNewerVersion(String newVersion, String currentVersion) {
        // Remove -SNAPSHOT suffix for comparison
        String cleanNewVersion = newVersion.replaceAll("-SNAPSHOT", "");
        String cleanCurrentVersion = currentVersion.replaceAll("-SNAPSHOT", "");
        
        // Split versions by dots to get major, minor, patch
        String[] newParts = cleanNewVersion.split("\\.");
        String[] currentParts = cleanCurrentVersion.split("\\.");
        
        // Compare each part of the version
        for (int i = 0; i < Math.max(newParts.length, currentParts.length); i++) {
            int newVal = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
            int currentVal = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            
            if (newVal > currentVal) {
                return true;
            } else if (newVal < currentVal) {
                return false;
            }
            // If equal, continue to next part
        }
        
        // If we get here, versions are equal, not newer
        return false;
    }
} 