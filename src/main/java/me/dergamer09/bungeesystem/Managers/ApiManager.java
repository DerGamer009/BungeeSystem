package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import me.dergamer09.bungeesystem.util.TokenGenerator;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Manages API communication with the BungeeSystem Dashboard
 */
public class ApiManager {
    
    private final BungeeSystem plugin;
    private String apiUrl;
    private String serverToken;
    private boolean apiEnabled;
    private int timeout;
    private int retryAttempts;
    private String serverId; // Cached server ID from API
    private String serverIp;
    private String serverPort;
    
    public ApiManager(BungeeSystem plugin) {
        this.plugin = plugin;
        
        // Load token configuration
        Configuration tokenConfig = loadTokenConfig();
        this.apiUrl = tokenConfig.getString("server.api_url", "https://api.devvoxel.net/");
        this.serverToken = tokenConfig.getString("server.token", "");
        this.apiEnabled = tokenConfig.getBoolean("api.enabled", true);
        this.timeout = tokenConfig.getInt("api.timeout", 5000);
        this.retryAttempts = tokenConfig.getInt("api.retry_attempts", 3);
        this.serverId = tokenConfig.getString("server.server_id", "");
        
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
        
        // Initialize server connection if API is enabled
        if (apiEnabled && !serverToken.isEmpty()) {
            initializeServerConnection();
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
     * Initialize server connection with the API (3-Step Login Flow)
     */
    private void initializeServerConnection() {
        CompletableFuture.runAsync(() -> {
            try {
                // Step 1: Send Server-IP + Port to POST /auth/server/connect
                if (!connectServer()) {
                    plugin.getLogger().warning("Failed to connect server to API");
                    return;
                }
                
                // Step 2: Send Token to POST /auth/server/token
                if (!authenticateToken()) {
                    plugin.getLogger().warning("Failed to authenticate token with API");
                    return;
                }
                
                plugin.getLogger().info("✅ Server successfully connected and authenticated with API");
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error during server connection initialization: " + e.getMessage());
            }
        });
    }
    
    /**
     * Step 1: Connect server to API
     */
    private boolean connectServer() {
        try {
            // Get server IP and port
            String serverIp = getServerIp();
            String serverPort = getServerPort();
            
            URL url = new URL(apiUrl + "/auth/server/connect");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-API-Key", serverToken);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setDoOutput(true);
            
            JSONObject connectData = new JSONObject();
            connectData.put("server_ip", serverIp);
            connectData.put("server_port", serverPort);
            connectData.put("server_type", "bungeecord");
            connectData.put("timestamp", System.currentTimeMillis());
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(connectData.toJSONString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Parse response to get server ID
                JSONParser parser = new JSONParser();
                JSONObject response = (JSONObject) parser.parse(new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                this.serverId = (String) response.get("server_id");
                
                // Save server ID to token config
                saveServerId();
                
                plugin.getLogger().info("✅ Server connected to API with ID: " + serverId);
                return true;
            } else {
                plugin.getLogger().warning("❌ Server connection failed - HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("❌ Server connection error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Step 2: Authenticate token with API
     */
    private boolean authenticateToken() {
        try {
            URL url = new URL(apiUrl + "/auth/server/token");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-API-Key", serverToken);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setDoOutput(true);
            
            JSONObject tokenData = new JSONObject();
            tokenData.put("token", serverToken);
            tokenData.put("server_id", serverId);
            tokenData.put("server_type", "bungeecord");
            tokenData.put("timestamp", System.currentTimeMillis());
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(tokenData.toJSONString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                plugin.getLogger().info("✅ Token authenticated successfully with API");
                return true;
            } else {
                plugin.getLogger().warning("❌ Token authentication failed - HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("❌ Token authentication error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get server IP (auto-detect or from config)
     */
    private String getServerIp() {
        Configuration config = plugin.getConfig();
        String configIp = config.getString("api.server-ip", "auto");
        
        if ("auto".equals(configIp)) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to auto-detect server IP: " + e.getMessage());
                return "127.0.0.1";
            }
        }
        return configIp;
    }
    
    /**
     * Get server port (auto-detect or from config)
     */
    private String getServerPort() {
        Configuration config = plugin.getConfig();
        String configPort = config.getString("api.port", "auto");
        
        if ("auto".equals(configPort)) {
            return String.valueOf(plugin.getProxy().getConfig().getListeners().iterator().next().getHost().getPort());
        }
        return configPort;
    }
    
    /**
     * Save server ID to token configuration
     */
    private void saveServerId() {
        try {
            File tokenFile = new File(plugin.getDataFolder(), "token.yml");
            Configuration tokenConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(tokenFile);
            tokenConfig.set("server.server_id", serverId);
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(tokenConfig, tokenFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save server ID: " + e.getMessage());
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
        banData.put("server_id", serverId);
        banData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/punishments/bans", banData, "POST");
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
        muteData.put("server_id", serverId);
        muteData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/punishments/mutes", muteData, "POST");
    }
    
    /**
     * Send a kick to the API
     */
    public void sendKick(String player, String reason, String admin) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject kickData = new JSONObject();
        kickData.put("player", player);
        kickData.put("reason", reason);
        kickData.put("admin", admin);
        kickData.put("server_id", serverId);
        kickData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/punishments/kicks", kickData, "POST");
    }
    
    /**
     * Send a warn to the API
     */
    public void sendWarn(String player, String reason, String admin) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject warnData = new JSONObject();
        warnData.put("player", player);
        warnData.put("reason", reason);
        warnData.put("admin", admin);
        warnData.put("server_id", serverId);
        warnData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/punishments/warns", warnData, "POST");
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
        reportData.put("server_id", serverId);
        reportData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/reports", reportData, "POST");
    }
    
    /**
     * Send player join event to API
     */
    public void sendPlayerJoin(String player, String uuid, String server) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject joinData = new JSONObject();
        joinData.put("player", player);
        joinData.put("uuid", uuid);
        joinData.put("server", server);
        joinData.put("server_id", serverId);
        joinData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/players/join", joinData, "POST");
    }
    
    /**
     * Send player quit event to API
     */
    public void sendPlayerQuit(String player, String uuid, String server) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject quitData = new JSONObject();
        quitData.put("player", player);
        quitData.put("uuid", uuid);
        quitData.put("server", server);
        quitData.put("server_id", serverId);
        quitData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/players/quit", quitData, "POST");
    }
    
    /**
     * Send player server switch event to API
     */
    public void sendPlayerSwitch(String player, String uuid, String fromServer, String toServer) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject switchData = new JSONObject();
        switchData.put("player", player);
        switchData.put("uuid", uuid);
        switchData.put("from_server", fromServer);
        switchData.put("to_server", toServer);
        switchData.put("server_id", serverId);
        switchData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/players/switch", switchData, "POST");
    }
    
    /**
     * Send admin action log to API
     */
    public void sendAdminLog(String admin, String action, String target, String details) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject logData = new JSONObject();
        logData.put("admin", admin);
        logData.put("action", action);
        logData.put("target", target);
        logData.put("details", details);
        logData.put("server_id", serverId);
        logData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/logs", logData, "POST");
    }
    
    /**
     * Send general log to API
     */
    public void sendGeneralLog(String level, String message, String category) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject logData = new JSONObject();
        logData.put("level", level);
        logData.put("message", message);
        logData.put("category", category);
        logData.put("server_id", serverId);
        logData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/logs", logData, "POST");
    }
    
    /**
     * Check if player is banned via API
     */
    public boolean checkPlayerBan(String player, String uuid) {
        if (!apiEnabled || serverToken.isEmpty()) return false;
        
        try {
            URL url = new URL(apiUrl + "/auth/players/" + uuid + "/banned");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-API-Key", serverToken);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                JSONParser parser = new JSONParser();
                JSONObject response = (JSONObject) parser.parse(new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                return (Boolean) response.get("banned");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking player ban: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if player is muted via API
     */
    public boolean checkPlayerMute(String player, String uuid) {
        if (!apiEnabled || serverToken.isEmpty()) return false;
        
        try {
            URL url = new URL(apiUrl + "/auth/players/" + uuid + "/muted");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-API-Key", serverToken);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                JSONParser parser = new JSONParser();
                JSONObject response = (JSONObject) parser.parse(new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                return (Boolean) response.get("muted");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking player mute: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get server ID
     */
    public String getServerId() {
        return serverId;
    }
    
    /**
     * Get API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }
    
    /**
     * Get server token
     */
    public String getServerToken() {
        return serverToken;
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
    
    /**
     * Validate server token with the backend
     * 
     * @return true if token is valid, false otherwise
     */
    public boolean validateServerToken() {
        if (!apiEnabled || serverToken.isEmpty()) {
            plugin.getLogger().warning("Cannot validate token - API disabled or no token configured");
            return false;
        }
        
        try {
            URL url = new URL(apiUrl + "/auth/servers/token");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-API-Key", serverToken);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setDoOutput(true);
            
            // Send token validation request
            JSONObject validationData = new JSONObject();
            validationData.put("token", serverToken);
            validationData.put("server_type", "bungeecord");
            validationData.put("timestamp", System.currentTimeMillis());
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(validationData.toJSONString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                plugin.getLogger().info("✅ Server token validated successfully with backend");
                return true;
            } else if (responseCode == 401) {
                plugin.getLogger().warning("❌ Server token validation failed - Invalid token");
                return false;
            } else if (responseCode == 404) {
                plugin.getLogger().warning("❌ Server token validation failed - Endpoint not found");
                return false;
            } else {
                plugin.getLogger().warning("❌ Server token validation failed - HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("❌ Server token validation error: " + e.getMessage());
            return false;
        }
    }
} 