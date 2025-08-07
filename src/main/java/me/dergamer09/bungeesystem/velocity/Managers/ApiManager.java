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
        
        // Load API configuration from config.yml (Velocity uses the same config structure)
        loadApiConfig();
        
        // Load token configuration
        loadTokenConfig();
        
        // Auto-generate token if none exists and API is enabled
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
        } else if (!apiEnabled) {
            plugin.getLogger().info("API Manager disabled - API features are disabled in config.yml");
        } else {
            plugin.getLogger().warn("API Manager disabled - no token configured");
        }
        
        // Initialize server connection if API is enabled
        if (apiEnabled && !serverToken.isEmpty()) {
            initializeServerConnection();
        }
    }
    
    /**
     * Load API configuration from config.yml
     */
    private void loadApiConfig() {
        try {
            // Try multiple possible paths for the config file
            Path[] possiblePaths = {
                Path.of("src/main/resources/config.yml"),
                Path.of("config.yml"),
                Path.of("plugins/BungeeSystem/config.yml"),
                Path.of("plugins/velocity/config.yml")
            };
            
            Path configFile = null;
            for (Path path : possiblePaths) {
                if (Files.exists(path)) {
                    configFile = path;
                    break;
                }
            }
            
            if (configFile != null) {
                String content = new String(Files.readAllBytes(configFile));
                String[] lines = content.split("\n");
                
                // Default values
                this.apiUrl = "http://api.devvoxel.net/";
                this.apiEnabled = false;
                this.timeout = 5000;
                this.retryAttempts = 3;
                
                // Parse configuration
                boolean inApiSection = false;
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("api:")) {
                        inApiSection = true;
                    } else if (inApiSection && line.startsWith("url:")) {
                        this.apiUrl = line.substring(line.indexOf(":") + 1).trim().replace("\"", "").replace("'", "");
                    } else if (inApiSection && line.startsWith("enabled:")) {
                        this.apiEnabled = line.contains("true");
                    } else if (inApiSection && line.startsWith("timeout:")) {
                        try {
                            String timeoutStr = line.substring(line.indexOf(":") + 1).trim();
                            if (timeoutStr.contains("#")) {
                                timeoutStr = timeoutStr.substring(0, timeoutStr.indexOf("#")).trim();
                            }
                            this.timeout = Integer.parseInt(timeoutStr);
                        } catch (NumberFormatException e) {
                            // Use default
                        }
                    } else if (inApiSection && line.startsWith("retry_attempts:")) {
                        try {
                            String retryStr = line.substring(line.indexOf(":") + 1).trim();
                            if (retryStr.contains("#")) {
                                retryStr = retryStr.substring(0, retryStr.indexOf("#")).trim();
                            }
                            this.retryAttempts = Integer.parseInt(retryStr);
                        } catch (NumberFormatException e) {
                            // Use default
                        }
                    } else if (inApiSection && !line.startsWith(" ") && !line.isEmpty()) {
                        // Exit API section
                        inApiSection = false;
                    }
                }
                
                plugin.getLogger().info("Loaded API config from: " + configFile.toString());
                plugin.getLogger().info("API enabled: " + this.apiEnabled);
            } else {
                // If config file doesn't exist, use hardcoded defaults
                this.apiUrl = "http://api.devvoxel.net/";
                this.apiEnabled = true; // Enable by default for Velocity
                this.timeout = 5000;
                this.retryAttempts = 3;
                plugin.getLogger().info("Config file not found in any location, using default API settings");
            }
        } catch (IOException e) {
            plugin.getLogger().warn("Failed to load config.yml: " + e.getMessage());
            // Use defaults
            this.apiUrl = "http://api.devvoxel.net/";
            this.apiEnabled = true; // Enable by default for Velocity
            this.timeout = 5000;
            this.retryAttempts = 3;
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
            this.serverToken = "";
            
            // Parse configuration
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("token:")) {
                    this.serverToken = line.substring(line.indexOf(":") + 1).trim().replace("\"", "").replace("'", "");
                }
            }
            
        } catch (IOException e) {
            plugin.getLogger().error("Failed to load token.yml: " + e.getMessage());
            // Set defaults
            this.serverToken = "";
        }
    }
    
    /**
     * Initialize server connection with the API (3-Step Login Flow)
     */
    private void initializeServerConnection() {
        CompletableFuture.runAsync(() -> {
            try {
                // First, test if the API is reachable
                if (!testApiConnection()) {
                    plugin.getLogger().warn("API server is not reachable. API features will be disabled.");
                    this.apiEnabled = false;
                    return;
                }
                
                // Step 1: Send Server-IP + Port to POST /auth/server/connect
                if (!connectServer()) {
                    plugin.getLogger().warn("Failed to connect server to API - server registration not implemented");
                    // Don't disable API completely, just skip server registration
                }
                
                // Step 2: Send Token to POST /auth/server/token
                if (!authenticateToken()) {
                    plugin.getLogger().warn("Failed to authenticate token with API - token authentication not implemented");
                    // Don't disable API completely, just skip token authentication
                }
                
                plugin.getLogger().info("✅ API connection test completed");
                
            } catch (Exception e) {
                plugin.getLogger().warn("Error during server connection initialization: " + e.getMessage());
            }
        });
    }
    
    /**
     * Test if the API is reachable
     */
    private boolean testApiConnection() {
        try {
            URL url = new URL(apiUrl + "/auth/servers");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                plugin.getLogger().info("✅ API server is reachable");
                return true;
            } else {
                plugin.getLogger().warn("❌ API server returned HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warn("❌ API connection test failed: " + e.getMessage());
            return false;
        }
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
    public void sendBan(String playerUuid, String playerUsername, String reason, String adminName, long duration) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject banData = new JSONObject();
        banData.put("playerUuid", playerUuid);
        banData.put("playerUsername", playerUsername);
        banData.put("reason", reason);
        banData.put("issuedBy", adminName);
        banData.put("serverId", serverId);
        banData.put("timestamp", System.currentTimeMillis());
        
        // Convert duration from seconds to ISO 8601 format if not permanent
        if (duration > 0) {
            long expirationTime = System.currentTimeMillis() + (duration * 1000);
            banData.put("expiresAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .format(new java.util.Date(expirationTime)));
        }
        
        sendApiRequest("/auth/punishments/bans", banData, "POST");
    }
    
    /**
     * Send a mute to the API
     */
    public void sendMute(String playerUuid, String playerUsername, String reason, String adminName, long duration) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject muteData = new JSONObject();
        muteData.put("playerUuid", playerUuid);
        muteData.put("playerUsername", playerUsername);
        muteData.put("reason", reason);
        muteData.put("issuedBy", adminName);
        muteData.put("serverId", serverId);
        muteData.put("timestamp", System.currentTimeMillis());
        
        // Convert duration from seconds to ISO 8601 format if not permanent
        if (duration > 0) {
            long expirationTime = System.currentTimeMillis() + (duration * 1000);
            muteData.put("expiresAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .format(new java.util.Date(expirationTime)));
        }
        
        sendApiRequest("/auth/punishments/mutes", muteData, "POST");
    }
    
    /**
     * Send a report to the API
     */
    public void sendReport(String reporterUuid, String reporterUsername, String targetUuid, String targetUsername, String reason, String serverName) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject reportData = new JSONObject();
        reportData.put("reporterUuid", reporterUuid);
        reportData.put("reporterUsername", reporterUsername);
        reportData.put("targetUuid", targetUuid);
        reportData.put("targetUsername", targetUsername);
        reportData.put("reason", reason);
        reportData.put("serverName", serverName);
        reportData.put("serverId", serverId);
        reportData.put("priority", "medium");
        reportData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/reports", reportData, "POST");
    }
    
    /**
     * Send a kick to the API
     */
    public void sendKick(String playerUuid, String playerUsername, String reason, String adminName) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject kickData = new JSONObject();
        kickData.put("playerUuid", playerUuid);
        kickData.put("playerUsername", playerUsername);
        kickData.put("reason", reason);
        kickData.put("issuedBy", adminName);
        kickData.put("serverId", serverId);
        kickData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/punishments/kicks", kickData, "POST");
    }
    
    /**
     * Send a warn to the API
     */
    public void sendWarn(String playerUuid, String playerUsername, String reason, String adminName, String severity) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject warnData = new JSONObject();
        warnData.put("playerUuid", playerUuid);
        warnData.put("playerUsername", playerUsername);
        warnData.put("reason", reason);
        warnData.put("issuedBy", adminName);
        warnData.put("serverId", serverId);
        warnData.put("severity", severity != null ? severity : "medium");
        warnData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/punishments/warns", warnData, "POST");
    }
    
    /**
     * Send player join event to API
     */
    public void sendPlayerJoin(String playerUsername, String playerUuid, String serverName) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject joinData = new JSONObject();
        joinData.put("playerUsername", playerUsername);
        joinData.put("playerUuid", playerUuid);
        joinData.put("serverName", serverName);
        joinData.put("serverId", serverId);
        joinData.put("action", "join");
        joinData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/players/events", joinData, "POST");
    }
    
    /**
     * Send player quit event to API
     */
    public void sendPlayerQuit(String playerUsername, String playerUuid, String serverName, long sessionDuration) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject quitData = new JSONObject();
        quitData.put("playerUsername", playerUsername);
        quitData.put("playerUuid", playerUuid);
        quitData.put("serverName", serverName);
        quitData.put("serverId", serverId);
        quitData.put("action", "quit");
        quitData.put("sessionDuration", sessionDuration);
        quitData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/players/events", quitData, "POST");
    }
    
    /**
     * Send player server switch event to API
     */
    public void sendPlayerSwitch(String playerUsername, String playerUuid, String fromServer, String toServer) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject switchData = new JSONObject();
        switchData.put("playerUsername", playerUsername);
        switchData.put("playerUuid", playerUuid);
        switchData.put("fromServer", fromServer);
        switchData.put("toServer", toServer);
        switchData.put("serverId", serverId);
        switchData.put("action", "switch");
        switchData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/players/events", switchData, "POST");
    }
    
    /**
     * Send server status update to API
     */
    public void sendServerStatus(int onlinePlayers, int maxPlayers, double tps, double cpuUsage, double ramUsage, String status) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject statusData = new JSONObject();
        statusData.put("serverId", serverId);
        statusData.put("onlinePlayers", onlinePlayers);
        statusData.put("maxPlayers", maxPlayers);
        statusData.put("tps", tps);
        statusData.put("cpuUsage", cpuUsage);
        statusData.put("ramUsage", ramUsage);
        statusData.put("status", status); // online, offline, maintenance
        statusData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/servers/" + serverId + "/status", statusData, "PUT");
    }
    
    /**
     * Send player statistics update to API
     */
    public void sendPlayerStats(String playerUuid, String playerUsername, long totalPlaytime, int totalLogins, int votes) {
        if (!apiEnabled || serverToken.isEmpty()) return;
        
        JSONObject statsData = new JSONObject();
        statsData.put("playerUuid", playerUuid);
        statsData.put("playerUsername", playerUsername);
        statsData.put("totalPlaytime", totalPlaytime);
        statsData.put("totalLogins", totalLogins);
        statsData.put("votes", votes);
        statsData.put("serverId", serverId);
        statsData.put("timestamp", System.currentTimeMillis());
        
        sendApiRequest("/auth/players/stats", statsData, "PUT");
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
     * Send API request asynchronously with improved error handling
     */
    private void sendApiRequest(String endpoint, JSONObject data, String method) {
        CompletableFuture.runAsync(() -> {
            for (int attempt = 1; attempt <= retryAttempts; attempt++) {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(apiUrl + endpoint);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod(method);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("User-Agent", "BungeeSystem-Velocity/" + plugin.getVersion());
                    
                    // Use server token for authentication
                    connection.setRequestProperty("X-API-Key", serverToken);
                    connection.setRequestProperty("X-Server-ID", serverId);
                    
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    
                    // Send data if present
                    if (data != null && !data.isEmpty()) {
                        try (OutputStream os = connection.getOutputStream()) {
                            os.write(data.toJSONString().getBytes(StandardCharsets.UTF_8));
                            os.flush();
                        }
                    }
                    
                    int responseCode = connection.getResponseCode();
                    
                    if (responseCode >= 200 && responseCode < 300) {
                        // Debug logging if enabled
                        try {
                            // Check if debug logging is enabled from config
                            boolean debugEnabled = true; // Default true for Velocity
                            if (debugEnabled) {
                                plugin.getLogger().info("✅ API request successful: " + method + " " + endpoint + " (HTTP " + responseCode + ")");
                            }
                        } catch (Exception ignored) {}
                        
                        // Read successful response if needed
                        try (java.io.InputStream inputStream = connection.getInputStream()) {
                            String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                            // Optional: Log response in debug mode
                        }
                        break;
                        
                    } else {
                        String errorMessage = "";
                        try (java.io.InputStream errorStream = connection.getErrorStream()) {
                            if (errorStream != null) {
                                errorMessage = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                            }
                        }
                        
                        plugin.getLogger().warn("❌ API request failed (attempt " + attempt + "/" + retryAttempts + 
                                "): " + method + " " + endpoint + " - HTTP " + responseCode + 
                                (errorMessage.isEmpty() ? "" : " - " + errorMessage));
                        
                        if (attempt == retryAttempts) {
                            plugin.getLogger().error("🚫 All API request attempts failed for: " + endpoint);
                            
                            // Special handling for authentication errors
                            if (responseCode == 401) {
                                plugin.getLogger().error("Authentication failed! Please check your server token.");
                                plugin.getLogger().info("Generate a new token with: /generatetoken force");
                            } else if (responseCode == 404 && endpoint.contains("/auth/servers/")) {
                                plugin.getLogger().error("Server not found in API! Please register your server.");
                            }
                        }
                        
                        // Exponential backoff for retries
                        if (attempt < retryAttempts) {
                            try {
                                Thread.sleep(1000 * attempt);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warn("❌ API request exception (attempt " + attempt + "/" + retryAttempts + 
                            "): " + endpoint + " - " + e.getMessage());
                    
                    if (attempt == retryAttempts) {
                        plugin.getLogger().error("🚫 All API request attempts failed due to exceptions for: " + endpoint);
                    }
                    
                    // Backoff for retries
                    if (attempt < retryAttempts) {
                        try {
                            Thread.sleep(2000 * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
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
        // Reload API settings from config.yml
        loadApiConfig();
        
        // Reload token from token.yml
        loadTokenConfig();
        
        if (apiEnabled && !serverToken.isEmpty()) {
            plugin.getLogger().info("API Manager reloaded with token: " + maskToken(serverToken));
        } else if (!apiEnabled) {
            plugin.getLogger().info("API Manager reloaded - API features are disabled in config.yml");
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