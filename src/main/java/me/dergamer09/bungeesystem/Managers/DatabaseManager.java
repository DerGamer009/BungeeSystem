package me.dergamer09.bungeesystem.Managers;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.config.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {

    private final Configuration config;
    private Connection connection;

    public DatabaseManager(Configuration config) {
        this.config = config;
    }

    public void connect() {
        String host = config.getString("mysql.host");
        String port = config.getString("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");
        
        // Get additional connection parameters with defaults if not present
        boolean useSSL = config.getBoolean("mysql.useSSL", false);
        boolean autoReconnect = config.getBoolean("mysql.autoReconnect", true);
        int connectionTimeout = config.getInt("mysql.connectionTimeout", 30000);
        int maxLifetime = config.getInt("mysql.maxLifetime", 1800000);
        int maximumPoolSize = config.getInt("mysql.maximumPoolSize", 10);
        int minimumIdle = config.getInt("mysql.minimumIdle", 5);

        // Build connection URL with parameters
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("jdbc:mysql://").append(host).append(":").append(port).append("/").append(database);
        urlBuilder.append("?useSSL=").append(useSSL);
        urlBuilder.append("&autoReconnect=").append(autoReconnect);
        urlBuilder.append("&connectTimeout=").append(connectionTimeout);
        urlBuilder.append("&characterEncoding=utf8");
        urlBuilder.append("&useUnicode=true");
        urlBuilder.append("&allowPublicKeyRetrieval=true");
        
        String url = urlBuilder.toString();

        try {
            // Register the driver (important for some environments)
            try {
                // Use only the newer driver class to avoid deprecation warnings
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] MySQL driver not found: " + ex.getMessage());
            }
            
            // Attempt the connection
            connection = DriverManager.getConnection(url, username, password);
            
            // Test if connection is valid
            if (connection.isValid(5)) {
                ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] MySQL connection established successfully.");
            } else {
                throw new SQLException("Connection established but not valid");
            }
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] MySQL connection failed: " + e.getMessage());
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Connection URL: " + url.replaceAll("password=.*?&", "password=****&"));
            e.printStackTrace();
        }
    }

    public void setupTables() {
        if (connection == null) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Cannot create tables, no MySQL connection!");
            return;
        }

        createBansTable();
        createMutesTable();
        createReportsTable();
        createOnlineTimeTable();
        createPlayerDataTable();
        createPunishmentReasonsTable();
        createWarningsTable();
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] MySQL connection closed.");
            } catch (SQLException e) {
                ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to close MySQL connection: " + e.getMessage());
            }
        }
    }

    private void createBansTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS bans (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "player_uuid VARCHAR(36) NOT NULL," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "banned_by VARCHAR(36) NOT NULL," +
                        "banned_by_name VARCHAR(16) NOT NULL," +
                        "reason_id INT NOT NULL," +
                        "reason TEXT," +
                        "timestamp BIGINT NOT NULL," +
                        "expire_timestamp BIGINT DEFAULT -1," +
                        "active BOOLEAN DEFAULT TRUE," +
                        "unbanned_by VARCHAR(36)," +
                        "unbanned_by_name VARCHAR(16)," +
                        "unban_timestamp BIGINT DEFAULT 0," +
                        "unban_reason TEXT," +
                        "ip VARCHAR(45)," +
                        "INDEX (player_uuid)," +
                        "INDEX (active))")) {
            ps.executeUpdate();
            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Bans table created or verified.");
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to create bans table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createMutesTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS mutes (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "player_uuid VARCHAR(36) NOT NULL," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "muted_by VARCHAR(36) NOT NULL," +
                        "muted_by_name VARCHAR(16) NOT NULL," +
                        "reason_id INT NOT NULL," +
                        "reason TEXT," +
                        "timestamp BIGINT NOT NULL," +
                        "expire_timestamp BIGINT DEFAULT -1," +
                        "active BOOLEAN DEFAULT TRUE," +
                        "unmuted_by VARCHAR(36)," +
                        "unmuted_by_name VARCHAR(16)," +
                        "unmute_timestamp BIGINT DEFAULT 0," +
                        "unmute_reason TEXT," +
                        "INDEX (player_uuid)," +
                        "INDEX (active))")) {
            ps.executeUpdate();
            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Mutes table created or verified.");
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to create mutes table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createReportsTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS reports (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "reported_uuid VARCHAR(36) NOT NULL," +
                        "reported_name VARCHAR(16) NOT NULL," +
                        "reported_by_uuid VARCHAR(36) NOT NULL," +
                        "reported_by_name VARCHAR(16) NOT NULL," +
                        "reason_id INT NOT NULL," +
                        "reason TEXT," +
                        "server VARCHAR(64)," +
                        "timestamp BIGINT NOT NULL," +
                        "resolved BOOLEAN DEFAULT FALSE," +
                        "resolved_by VARCHAR(36)," +
                        "resolved_by_name VARCHAR(16)," +
                        "resolve_timestamp BIGINT DEFAULT 0," +
                        "resolve_notes TEXT," +
                        "INDEX (reported_uuid)," +
                        "INDEX (resolved))")) {
            ps.executeUpdate();
            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Reports table created or verified.");
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to create reports table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createWarningsTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS warnings (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "player_uuid VARCHAR(36) NOT NULL," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "warned_by VARCHAR(36) NOT NULL," +
                        "warned_by_name VARCHAR(16) NOT NULL," +
                        "reason_id INT NOT NULL," +
                        "reason TEXT," +
                        "timestamp BIGINT NOT NULL," +
                        "expire_timestamp BIGINT DEFAULT -1," +
                        "active BOOLEAN DEFAULT TRUE," +
                        "seen BOOLEAN DEFAULT FALSE," +
                        "seen_timestamp BIGINT DEFAULT 0," +
                        "removed_by VARCHAR(36)," +
                        "removed_by_name VARCHAR(16)," +
                        "remove_timestamp BIGINT DEFAULT 0," +
                        "remove_reason TEXT," +
                        "INDEX (player_uuid)," +
                        "INDEX (active))")) {
            ps.executeUpdate();
            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Warnings table created or verified.");
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to create warnings table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createPunishmentReasonsTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS punishment_reasons (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "name VARCHAR(64) NOT NULL," +
                        "type ENUM('BAN', 'MUTE', 'WARN', 'REPORT') NOT NULL," +
                        "description TEXT," +
                        "duration BIGINT DEFAULT -1," +  // -1 = permanent
                        "created_by VARCHAR(36)," +
                        "created_timestamp BIGINT," +
                        "last_updated_by VARCHAR(36)," +
                        "last_updated_timestamp BIGINT," +
                        "UNIQUE KEY (name, type))")) {
            ps.executeUpdate();
            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Punishment reasons table created or verified.");
            
            // Add default punishment reasons if table is empty
            addDefaultPunishmentReasons();
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to create punishment reasons table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addDefaultPunishmentReasons() {
        try {
            PreparedStatement checkPS = connection.prepareStatement(
                    "SELECT COUNT(*) FROM punishment_reasons");
            ResultSet rs = checkPS.executeQuery();
            
            if (rs.next() && rs.getInt(1) == 0) {
                // Table is empty, add default reasons
                long now = System.currentTimeMillis();
                
                // Common reasons across all types
                String[][] commonReasons = {
                    {"Hacking/Cheating", "Using unauthorized modifications or cheats", "259200000"}, // 3 days
                    {"Inappropriate Language", "Using offensive or inappropriate language", "86400000"}, // 1 day
                    {"Spamming", "Repeatedly sending the same message", "3600000"}, // 1 hour
                    {"Other", "Other reason not listed", "-1"} // Permanent
                };
                
                // Add ban reasons
                for (String[] reason : commonReasons) {
                    addReason("BAN", reason[0], reason[1], Long.parseLong(reason[2]), now);
                }
                addReason("BAN", "Griefing", "Intentional destruction of other players' property", 604800000L, now); // 7 days
                addReason("BAN", "Advertising", "Promoting other servers or services", 1209600000L, now); // 14 days
                
                // Add mute reasons
                for (String[] reason : commonReasons) {
                    addReason("MUTE", reason[0], reason[1], Long.parseLong(reason[2]), now);
                }
                addReason("MUTE", "Harassment", "Targeting specific players with unwanted behavior", 172800000L, now); // 2 days
                
                // Add warn reasons
                for (String[] reason : commonReasons) {
                    addReason("WARN", reason[0], reason[1], Long.parseLong(reason[2]), now);
                }
                addReason("WARN", "Rule Violation", "Breaking server rules", 604800000L, now); // 7 days
                
                // Add report reasons
                for (String[] reason : commonReasons) {
                    addReason("REPORT", reason[0], reason[1], -1L, now); // Reports don't use duration
                }
                addReason("REPORT", "Harassment", "Targeting specific players with unwanted behavior", -1L, now);
                addReason("REPORT", "Suspicious Behavior", "Player is acting suspiciously", -1L, now);
                
                ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Added default punishment reasons.");
            }
            
            rs.close();
            checkPS.close();
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to add default punishment reasons: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addReason(String type, String name, String description, long duration, long timestamp) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO punishment_reasons (name, type, description, duration, created_timestamp) VALUES (?, ?, ?, ?, ?)");
        ps.setString(1, name);
        ps.setString(2, type);
        ps.setString(3, description);
        ps.setLong(4, duration);
        ps.setLong(5, timestamp);
        ps.executeUpdate();
        ps.close();
    }

    private void createOnlineTimeTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS online_time (" +
                        "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                        "total_time BIGINT NOT NULL DEFAULT 0," +
                        "last_login BIGINT DEFAULT 0)")) {
            ps.executeUpdate();
            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Online time table created or verified.");
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to create online time table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createPlayerDataTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "name VARCHAR(16) NOT NULL, " +
                        "ip VARCHAR(45), " +
                        "first_join BIGINT, " +
                        "last_seen BIGINT)")) {
            ps.executeUpdate();
            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Player data table created or verified.");
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to create player data table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get a player's UUID from their name
     * 
     * @param playerName The name of the player
     * @return The UUID of the player, or null if not found
     */
    public UUID getUUIDFromName(String playerName) {
        if (connection == null) {
            return null;
        }
        
        try {
            // First check player_data table
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid FROM player_data WHERE name = ? LIMIT 1");
            ps.setString(1, playerName);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String uuidString = rs.getString("uuid");
                rs.close();
                ps.close();
                return UUID.fromString(uuidString);
            }
            
            rs.close();
            ps.close();
            
            // Then check player_stats table as a fallback
            ps = connection.prepareStatement(
                    "SELECT uuid FROM player_stats WHERE name = ? LIMIT 1");
            ps.setString(1, playerName);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String uuidString = rs.getString("uuid");
                rs.close();
                ps.close();
                return UUID.fromString(uuidString);
            }
            
            rs.close();
            ps.close();
            
            // Player not found in either table
            return null;
            
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("Failed to get UUID for player " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Test the database connection and return diagnostic information
     * 
     * @return true if connection is valid, false otherwise
     */
    public boolean testConnection() {
        ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Testing database connection...");
        
        String host = config.getString("mysql.host");
        String port = config.getString("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        
        try {
            // Check if we can resolve the hostname
            ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Checking if host " + host + " is reachable...");
            boolean reachable = false;
            try {
                java.net.InetAddress address = java.net.InetAddress.getByName(host);
                reachable = address.isReachable(5000);
                ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Host IP: " + address.getHostAddress());
                ProxyServer.getInstance().getLogger().info(reachable ? 
                        "§a[BungeeSystem] Host is reachable." : 
                        "§c[BungeeSystem] Host is not reachable!");
            } catch (Exception e) {
                ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Cannot resolve host: " + e.getMessage());
            }
            
            // Try to open a socket to the port
            ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Checking if MySQL port " + port + " is open...");
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, Integer.parseInt(port)), 5000);
                ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Successfully connected to port " + port);
            } catch (Exception e) {
                ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Cannot connect to port: " + e.getMessage());
            }
            
            // Test database credentials
            if (connection != null) {
                ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Testing if connection is valid...");
                boolean valid = connection.isValid(5);
                ProxyServer.getInstance().getLogger().info(valid ? 
                        "§a[BungeeSystem] Connection is valid." : 
                        "§c[BungeeSystem] Connection is not valid!");
                
                // Try a simple query
                ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Testing simple query...");
                try (PreparedStatement ps = connection.prepareStatement("SELECT 1")) {
                    ResultSet rs = ps.executeQuery();
                    boolean hasResult = rs.next();
                    rs.close();
                    ProxyServer.getInstance().getLogger().info(hasResult ? 
                            "§a[BungeeSystem] Query executed successfully." : 
                            "§c[BungeeSystem] Query failed to return results!");
                } catch (SQLException e) {
                    ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Query failed: " + e.getMessage());
                }
                
                // Check database existence
                ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Checking if database exists...");
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?")) {
                    ps.setString(1, database);
                    ResultSet rs = ps.executeQuery();
                    boolean exists = rs.next();
                    rs.close();
                    ProxyServer.getInstance().getLogger().info(exists ? 
                            "§a[BungeeSystem] Database '" + database + "' exists." : 
                            "§c[BungeeSystem] Database '" + database + "' does not exist!");
                } catch (SQLException e) {
                    ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to check database existence: " + e.getMessage());
                }
                
                // Check privileges
                ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Checking user privileges...");
                try (PreparedStatement ps = connection.prepareStatement(
                        "SHOW GRANTS FOR CURRENT_USER()")) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] " + rs.getString(1));
                    }
                    rs.close();
                } catch (SQLException e) {
                    ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to check privileges: " + e.getMessage());
                }
                
                return valid;
            } else {
                ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] No connection established to test!");
                return false;
            }
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Connection test failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Attempt to create the database if it doesn't exist
     * 
     * @return true if successful or database already exists
     */
    public boolean createDatabaseIfNotExists() {
        String host = config.getString("mysql.host");
        String port = config.getString("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");
        
        // Build the connection string without specifying a database
        String url = "jdbc:mysql://" + host + ":" + port + "?useSSL=false&autoReconnect=true";
        
        try {
            ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Checking if database exists...");
            
            // Connect without specifying a database
            try (Connection rootConnection = DriverManager.getConnection(url, username, password)) {
                // Check if database exists
                try (PreparedStatement ps = rootConnection.prepareStatement(
                        "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?")) {
                    ps.setString(1, database);
                    ResultSet rs = ps.executeQuery();
                    boolean exists = rs.next();
                    rs.close();
                    
                    if (!exists) {
                        ProxyServer.getInstance().getLogger().info("§7[BungeeSystem] Database '" + database + 
                                "' does not exist. Attempting to create it...");
                        
                        // Create the database
                        try (PreparedStatement createPs = rootConnection.prepareStatement(
                                "CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")) {
                            createPs.executeUpdate();
                            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Database '" + database + 
                                    "' created successfully.");
                            return true;
                        } catch (SQLException e) {
                            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to create database: " + e.getMessage());
                            return false;
                        }
                    } else {
                        ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Database '" + database + "' already exists.");
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Failed to check/create database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Initialize the database connection with fallback options
     * 
     * @return true if connection established
     */
    public boolean initialize() {
        // First try to connect normally
        connect();
        
        // If connection failed, try to create the database
        if (connection == null) {
            ProxyServer.getInstance().getLogger().warning("§e[BungeeSystem] Initial connection failed. Checking if database needs to be created...");
            
            if (createDatabaseIfNotExists()) {
                // Try connecting again after database creation
                connect();
                
                if (connection != null) {
                    ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] Successfully connected after creating database.");
                    return true;
                } else {
                    ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] Still unable to connect after creating database.");
                    return false;
                }
            } else {
                return false;
            }
        }
        
        return connection != null;
    }
}