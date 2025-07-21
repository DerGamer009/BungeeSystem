package me.dergamer09.bungeesystem.velocity.Managers;

import com.velocitypowered.api.proxy.ProxyServer;
import net.md_5.bungee.config.Configuration;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private final Configuration config;
    private final Logger logger;
    private final ProxyServer server;
    private Connection connection;

    public DatabaseManager(Configuration config, Logger logger, ProxyServer server) {
        this.config = config;
        this.logger = logger;
        this.server = server;
    }

    public boolean initialize() {
        connect();
        if (connection != null) {
            createMaintenanceWhitelistTable();
            createPlayerStatsTable();
            createOnlineTimeTable();
            createPlayerDataTable();
        }
        return connection != null;
    }

    private void connect() {
        String host = config.getString("mysql.host");
        String port = config.getString("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");
        boolean useSSL = config.getBoolean("mysql.useSSL", false);
        boolean autoReconnect = config.getBoolean("mysql.autoReconnect", true);

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=" + useSSL + "&autoReconnect=" + autoReconnect +
                "&characterEncoding=utf8&useUnicode=true&allowPublicKeyRetrieval=true";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, username, password);
            if (connection.isValid(5)) {
                logger.info("MySQL connection established.");
            }
        } catch (Exception e) {
            logger.error("MySQL connection failed: {}", e.getMessage());
        }
    }

    private void createMaintenanceWhitelistTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS maintenance_whitelist (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "added BIGINT NOT NULL)")) {
            ps.executeUpdate();
            logger.info("Maintenance whitelist table created or verified.");
        } catch (SQLException e) {
            logger.error("Failed to create maintenance whitelist table: {}", e.getMessage());
        }
    }

    private void createPlayerStatsTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "name VARCHAR(16) NOT NULL," +
                        "first_join BIGINT," +
                        "last_join BIGINT," +
                        "login_count INT DEFAULT 0," +
                        "votes INT DEFAULT 0," +
                        "messages_sent INT DEFAULT 0" +
                        ")")) {
            ps.executeUpdate();
            logger.info("Player stats table created or verified.");
        } catch (SQLException e) {
            logger.error("Failed to create player stats table: {}", e.getMessage());
        }
    }

    private void createOnlineTimeTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS online_time (" +
                        "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                        "total_time BIGINT NOT NULL DEFAULT 0," +
                        "last_login BIGINT DEFAULT 0)")) {
            ps.executeUpdate();
            logger.info("Online time table created or verified.");
        } catch (SQLException e) {
            logger.error("Failed to create online time table: {}", e.getMessage());
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
            logger.info("Player data table created or verified.");
        } catch (SQLException e) {
            logger.error("Failed to create player data table: {}", e.getMessage());
        }
    }

    public void addToWhitelist(UUID uuid) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "REPLACE INTO maintenance_whitelist (uuid, added) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to add whitelist entry: {}", e.getMessage());
        }
    }

    public void removeFromWhitelist(UUID uuid) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM maintenance_whitelist WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to remove whitelist entry: {}", e.getMessage());
        }
    }

    public boolean isInWhitelist(UUID uuid) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid FROM maintenance_whitelist WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check whitelist: {}", e.getMessage());
            return false;
        }
    }

    public List<UUID> getWhitelist() {
        List<UUID> list = new ArrayList<>();
        if (connection == null) return list;
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM maintenance_whitelist")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        list.add(UUID.fromString(rs.getString("uuid")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to read whitelist: {}", e.getMessage());
        }
        return list;
    }

    /**
     * Retrieve a player's UUID from the database using their name.
     *
     * @param playerName the player's name
     * @return UUID of the player or null if not found
     */
    public UUID getUUIDFromName(String playerName) {
        if (connection == null) {
            return null;
        }
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid FROM player_data WHERE name = ? LIMIT 1");
            ps.setString(1, playerName);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                rs.close();
                ps.close();
                return uuid;
            }
            rs.close();
            ps.close();

            ps = connection.prepareStatement(
                    "SELECT uuid FROM player_stats WHERE name = ? LIMIT 1");
            ps.setString(1, playerName);
            rs = ps.executeQuery();
            if (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                rs.close();
                ps.close();
                return uuid;
            }
            rs.close();
            ps.close();
            return null;
        } catch (SQLException e) {
            logger.error("Failed to get UUID for player {}: {}", playerName, e.getMessage());
            return null;
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
