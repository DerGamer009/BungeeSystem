package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.util.TokenGenerator;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Manages API communication with the BungeeSystem Dashboard
 */
public class ApiManager {
    
    private final BungeeSystem plugin;
    private final String apiUrl;
    private String serverToken;
    private boolean apiEnabled;
    private final int timeout;
    private final int retryAttempts;
    
    public ApiManager(BungeeSystem plugin) {
        this.plugin = plugin;
        
        // Load token configuration
        Configuration tokenConfig = loadTokenConfig();
        this.apiUrl = tokenConfig.getString("server.api_url", "http://45.86.155.38:25664");
        this.serverToken = tokenConfig.getString("server.token", "");
        this.apiEnabled = tokenConfig.getBoolean("api.enabled", true);
        this.timeout = tokenConfig.getInt("api.timeout", 5000);
        this.retryAttempts = tokenConfig.getInt("api.retry_attempts", 3);
        
        // Auto-generate token if none exists
        if (apiEnabled && (serverToken.isEmpty() || serverToken.equals("your_server_token_here"))) {
            plugin.getLogger().info("No valid token found. Generating new server token...");
            String newToken = TokenGenerator.generateAndSaveToken(plugin);
            if (newToken != null) {
                this.serverToken = newToken;
                plugin.getLogger().info("New token generated and saved. API Manager initialized with token: " + maskToken(serverToken));
            } else {
                plugin.getLogger().warning("Failed to generate token. API Manager disabled.");
                this.apiEnabled = false;
            }
        } else if (apiEnabled && !serverToken.isEmpty()) {
            plugin.getLogger().info("API Manager initialized with existing token: " + maskToken(serverToken));
        } else {
            plugin.getLogger().warning("API Manager disabled - no token configured or API disabled");
        }
    }
    
    /**
     * Load token configuration from token.yml
     */
    private Configuration loadTokenConfig() {
        try {
            File tokenFile = new File(plugin.getDataFolder(), "token.yml");
            if (!tokenFile.exists()) {
                // Create default token.yml if it doesn't exist
                plugin.getDataFolder().mkdirs();
                tokenFile.createNewFile();
            }
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(tokenFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load token.yml: " + e.getMessage());
            return new Configuration();
        }
    }
    
    /**
     * Send a ban to the API
     */
    public void sendBan(String player, String reason, String admin, long duration) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject banData = new JSONObject();
        banData.put("player", player);
        banData.put("reason", reason);
        banData.put("admin", admin);
        banData.put("duration", duration);
        banData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/api/bans", banData, "POST");
    }
    
    /**
     * Send a mute to the API
     */
    public void sendMute(String player, String reason, String admin, long duration) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject muteData = new JSONObject();
        muteData.put("player", player);
        muteData.put("reason", reason);
        muteData.put("admin", admin);
        muteData.put("duration", duration);
        muteData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/api/mutes", muteData, "POST");
    }
    
    /**
     * Send a report to the API
     */
    public void sendReport(String reporter, String target, String reason, String server) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject reportData = new JSONObject();
        reportData.put("reporter", reporter);
        reportData.put("target", target);
        reportData.put("reason", reason);
        reportData.put("server", server);
        reportData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/api/reports", reportData, "POST");
    }
    
    /**
     * Send API request asynchronously
     */
    private void sendApiRequest(String endpoint, JSONObject data, String method) {
        CompletableFuture.runAsync(() -> {
            for (int attempt = 1; attempt <= retryAttempts; attempt++) {
                try {
                    URL url = new URL(apiUrl + endpoint);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod(method);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("X-API-Key", serverToken);
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);
                    connection.setDoOutput(true);
                    
                    // Send data
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(data.toJSONString().getBytes(StandardCharsets.UTF_8));
                    }
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        plugin.getLogger().info("API request successful: " + endpoint);
                        break;
                    } else {
                        plugin.getLogger().warning("API request failed (attempt " + attempt + "/" + retryAttempts + 
                                "): " + endpoint + " - HTTP " + responseCode);
                        if (attempt == retryAttempts) {
                            plugin.getLogger().severe("All API request attempts failed for: " + endpoint);
                        }
                    }
                    
                    connection.disconnect();
                } catch (Exception e) {
                    plugin.getLogger().warning("API request error (attempt " + attempt + "/" + retryAttempts + 
                            "): " + e.getMessage());
                    if (attempt == retryAttempts) {
                        plugin.getLogger().severe("All API request attempts failed for: " + endpoint + " - " + e.getMessage());
                    }
                }
                
                // Wait before retry
                if (attempt < retryAttempts) {
                    try {
                        Thread.sleep(1000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }
    
    /**
     * Check API health
     */
    public void checkApiHealth() {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(apiUrl + "/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    plugin.getLogger().info("API health check successful");
                } else {
                    plugin.getLogger().warning("API health check failed: HTTP " + responseCode);
                }
                
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("API health check error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Mask token for logging
     */
    private String maskToken(String token) {
        if (token.length() <= 8) return "***";
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
    
    /**
     * Check if API is enabled
     */
    public boolean isApiEnabled() {
        return apiEnabled && !serverToken.isEmpty();
    }
    
    /**
     * Reload token from configuration
     */
    public void reloadToken() {
        Configuration tokenConfig = loadTokenConfig();
        this.serverToken = tokenConfig.getString("server.token", "");
        this.apiEnabled = tokenConfig.getBoolean("api.enabled", true);
        
        if (apiEnabled && !serverToken.isEmpty()) {
            plugin.getLogger().info("API Manager reloaded with token: " + maskToken(serverToken));
        } else {
            plugin.getLogger().warning("API Manager reloaded but no valid token found");
        }
    }
} 