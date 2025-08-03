package me.dergamer09.bungeesystem.velocity.Managers;

import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import me.dergamer09.bungeesystem.velocity.util.TokenGenerator;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Manages API communication with the VelocitySystem Dashboard
 */
public class ApiManager {
    
    private final VelocitySystem plugin;
    private String apiUrl;
    private String serverToken;
    private boolean apiEnabled;
    private int timeout;
    private int retryAttempts;
    private String serverId; // Cached server ID from API
    private String serverIp;
    private String serverPort;
    
    public ApiManager(VelocitySystem plugin) {
        this.plugin = plugin;
        
        // Load token configuration
        loadTokenConfig();
        
        // Auto-generate token if none exists
        if (apiEnabled && (serverToken.isEmpty() || serverToken.equals("your_server_token_here"))) {
            plugin.getLogger().info("No valid token found. Generating new server token...");
            String newToken = TokenGenerator.generateAndSaveToken(plugin);
            if (newToken != null) {
                this.serverToken = newToken;
                plugin.getLogger().info("New token generated and saved. API Manager initialized with token: " + maskToken(serverToken));
            } else {
                plugin.getLogger().warn("Failed to generate token. API Manager disabled.");
                this.apiEnabled = false;
            }
        } else if (apiEnabled && !serverToken.isEmpty()) {
            plugin.getLogger().info("API Manager initialized with existing token: " + maskToken(serverToken));
        } else {
            plugin.getLogger().warn("API Manager disabled - no token configured or API disabled");
        }
        
        // Initialize server connection if API is enabled
        if (apiEnabled && !serverToken.isEmpty()) {
            initializeServerConnection();
        }
    }
    
    /**
     * Load token configuration from token.yml
     */
    private void loadTokenConfig() {
        try {
            Path tokenFile = plugin.getDataDirectory().resolve("token.yml");
            if (!Files.exists(tokenFile)) {
                // Create default token.yml if it doesn't exist
                Files.createDirectories(plugin.getDataDirectory());
                Files.createFile(tokenFile);
            }
            
            String content = new String(Files.readAllBytes(tokenFile));
            String[] lines = content.split("\n");
            
            // Default values
            this.apiUrl = "http://45.86.155.38:25664";
            this.serverToken = "";
            this.apiEnabled = true;
            this.timeout = 5000;
            this.retryAttempts = 3;
            
            // Parse configuration
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("token:")) {
                    this.serverToken = line.substring(line.indexOf(":") + 1).trim().replace("\"", "").replace("'", "");
                } else if (line.startsWith("api_url:")) {
                    // Parse API URL if present
                } else if (line.startsWith("enabled:")) {
                    this.apiEnabled = line.contains("true");
                } else if (line.startsWith("timeout:")) {
                    try {
                        String timeoutStr = line.substring(line.indexOf(":") + 1).trim();
                        if (timeoutStr.contains("#")) {
                            timeoutStr = timeoutStr.substring(0, timeoutStr.indexOf("#")).trim();
                        }
                        this.timeout = Integer.parseInt(timeoutStr);
                    } catch (NumberFormatException e) {
                        // Use default
                    }
                } else if (line.startsWith("retry_attempts:")) {
                    try {
                        String retryStr = line.substring(line.indexOf(":") + 1).trim();
                        if (retryStr.contains("#")) {
                            retryStr = retryStr.substring(0, retryStr.indexOf("#")).trim();
                        }
                        this.retryAttempts = Integer.parseInt(retryStr);
                    } catch (NumberFormatException e) {
                        // Use default
                    }
                }
            }
            
        } catch (IOException e) {
            plugin.getLogger().error("Failed to load token.yml: " + e.getMessage());
            // Set defaults
            this.apiUrl = "http://45.86.155.38:25664";
            this.serverToken = "";
            this.apiEnabled = true;
            this.timeout = 5000;
            this.retryAttempts = 3;
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
                    plugin.getLogger().warn("Failed to connect server to API");
                    return;
                }
                
                // Step 2: Send Token to POST /auth/server/token
                if (!authenticateToken()) {
                    plugin.getLogger().warn("Failed to authenticate token with API");
                    return;
                }
                
                plugin.getLogger().info("✅ Server successfully connected and authenticated with API");
                
            } catch (Exception e) {
                plugin.getLogger().warn("Error during server connection initialization: " + e.getMessage());
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
            connectData.put("server_type", "velocity");
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
                plugin.getLogger().warn("❌ Server connection failed - HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warn("❌ Server connection error: " + e.getMessage());
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
            tokenData.put("server_type", "velocity");
            tokenData.put("timestamp", System.currentTimeMillis());
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(tokenData.toJSONString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                plugin.getLogger().info("✅ Token authenticated successfully with API");
                return true;
            } else {
                plugin.getLogger().warn("❌ Token authentication failed - HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warn("❌ Token authentication error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get server IP (auto-detect or from config)
     */
    private String getServerIp() {
        // For Velocity, we'll use auto-detection
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to auto-detect server IP: " + e.getMessage());
            return "127.0.0.1";
        }
    }
    
    /**
     * Get server port (auto-detect or from config)
     */
    private String getServerPort() {
        // For Velocity, we'll use the configured port
        return String.valueOf(plugin.getServer().getBoundAddress().getPort());
    }
    
    /**
     * Save server ID to token configuration
     */
    private void saveServerId() {
        try {
            Path tokenFile = plugin.getDataDirectory().resolve("token.yml");
            String content = new String(Files.readAllBytes(tokenFile));
            
            // Add or update server_id in the content
            if (content.contains("server_id:")) {
                content = content.replaceAll("server_id:.*", "server_id: \"" + serverId + "\"");
            } else {
                content += "\nserver_id: \"" + serverId + "\"";
            }
            
            Files.write(tokenFile, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warn("Failed to save server ID: " + e.getMessage());
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
            plugin.getLogger().warn("Error checking player ban: " + e.getMessage());
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
            plugin.getLogger().warn("Error checking player mute: " + e.getMessage());
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
                        plugin.getLogger().warn("API request failed (attempt " + attempt + "/" + retryAttempts + 
                                "): " + endpoint + " - HTTP " + responseCode);
                        if (attempt == retryAttempts) {
                            plugin.getLogger().error("All API request attempts failed for: " + endpoint);
                        }
                    }
                    
                    connection.disconnect();
                } catch (Exception e) {
                    plugin.getLogger().warn("API request error (attempt " + attempt + "/" + retryAttempts + 
                            "): " + e.getMessage());
                    if (attempt == retryAttempts) {
                        plugin.getLogger().error("All API request attempts failed for: " + endpoint + " - " + e.getMessage());
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
                    plugin.getLogger().warn("API health check failed: HTTP " + responseCode);
                }
                
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warn("API health check error: " + e.getMessage());
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
        loadTokenConfig();
        
        if (apiEnabled && !serverToken.isEmpty()) {
            plugin.getLogger().info("API Manager reloaded with token: " + maskToken(serverToken));
        } else {
            plugin.getLogger().warn("API Manager reloaded but no valid token found");
        }
    }
    
    /**
     * Validate server token with the backend
     * 
     * @return true if token is valid, false otherwise
     */
    public boolean validateServerToken() {
        if (!apiEnabled || serverToken.isEmpty()) {
            plugin.getLogger().warn("Cannot validate token - API disabled or no token configured");
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
            validationData.put("server_type", "velocity");
            validationData.put("timestamp", System.currentTimeMillis());
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(validationData.toJSONString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                plugin.getLogger().info("✅ Server token validated successfully with backend");
                return true;
            } else if (responseCode == 401) {
                plugin.getLogger().warn("❌ Server token validation failed - Invalid token");
                return false;
            } else if (responseCode == 404) {
                plugin.getLogger().warn("❌ Server token validation failed - Endpoint not found");
                return false;
            } else {
                plugin.getLogger().warn("❌ Server token validation failed - HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warn("❌ Server token validation error: " + e.getMessage());
            return false;
        }
    }
} 