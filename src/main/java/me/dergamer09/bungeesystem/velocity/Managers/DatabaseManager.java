package me.dergamer09.bungeesystem.velocity.Managers;

import com.velocitypowered.api.proxy.ProxyServer;
import net.md_5.bungee.config.Configuration;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;

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
            createPunishmentReasonsTable();
            createReportsTable();
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
                        "status ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN'," +
                        "resolved BOOLEAN DEFAULT FALSE," +
                        "resolved_by VARCHAR(36)," +
                        "resolved_by_name VARCHAR(16)," +
                        "resolve_timestamp BIGINT DEFAULT 0," +
                        "resolve_notes TEXT," +
                        "INDEX (reported_uuid)," +
                        "INDEX (resolved))")) {
            ps.executeUpdate();
            logger.info("Reports table created or verified.");
        } catch (SQLException e) {
            logger.error("Failed to create reports table: {}", e.getMessage());
        }
    }

    private void createPunishmentReasonsTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS punishment_reasons (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "name VARCHAR(64) NOT NULL," +
                        "type ENUM('BAN', 'MUTE', 'WARN', 'REPORT') NOT NULL," +
                        "description TEXT," +
                        "duration BIGINT DEFAULT -1," +
                        "created_by VARCHAR(36)," +
                        "created_timestamp BIGINT," +
                        "last_updated_by VARCHAR(36)," +
                        "last_updated_timestamp BIGINT," +
                        "UNIQUE KEY (name, type))")) {
            ps.executeUpdate();
            logger.info("Punishment reasons table created or verified.");
        } catch (SQLException e) {
            logger.error("Failed to create punishment reasons table: {}", e.getMessage());
        }
        // Default reasons einfügen, falls Tabelle leer ist
        try (PreparedStatement checkPS = connection.prepareStatement(
                "SELECT COUNT(*) FROM punishment_reasons")) {
            ResultSet rs = checkPS.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                long now = System.currentTimeMillis();
                String[][] commonReasons = {
                    {"Hacking/Cheating", "Using unauthorized modifications or cheats", "-1"},
                    {"Inappropriate Language", "Using offensive or inappropriate language", "-1"},
                    {"Spamming", "Repeatedly sending the same message", "-1"},
                    {"Other", "Other reason not listed", "-1"}
                };
                for (String[] reason : commonReasons) {
                    addReason("REPORT", reason[0], reason[1], -1L, now);
                }
                addReason("REPORT", "Harassment", "Targeting specific players with unwanted behavior", -1L, now);
                addReason("REPORT", "Suspicious Behavior", "Player is acting suspiciously", -1L, now);
                logger.info("Default report reasons added.");
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Failed to add default punishment reasons: {}", e.getMessage());
        }
    }

    private void addReason(String type, String name, String description, long duration, long timestamp) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO punishment_reasons (name, type, description, duration, created_timestamp) VALUES (?, ?, ?, ?, ?)");) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, description);
            ps.setLong(4, duration);
            ps.setLong(5, timestamp);
            ps.executeUpdate();
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
    
    /**
     * Get UUID from player name
     */
    public UUID getUUIDFromName(String playerName) {
        if (connection == null) return null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid FROM player_data WHERE LOWER(username) = LOWER(?) LIMIT 1")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get UUID for player {}: {}", playerName, e.getMessage());
        }
        return null;
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

    public List<Map<String, Object>> getReasons(String type) {
        List<Map<String, Object>> reasons = new ArrayList<>();
        if (connection == null) return reasons;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, name, description FROM punishment_reasons WHERE type = ?")) {
            ps.setString(1, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> reason = new java.util.HashMap<>();
                reason.put("id", rs.getInt("id"));
                reason.put("name", rs.getString("name"));
                reason.put("description", rs.getString("description"));
                reasons.add(reason);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Failed to get reasons: {}", e.getMessage());
        }
        return reasons;
    }

    public boolean insertReport(UUID reportedUUID, String reportedName, UUID reporterUUID, String reporterName, int reasonId, String reason, String server, long timestamp) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO reports (reported_uuid, reported_name, reported_by_uuid, reported_by_name, reason_id, reason, server, timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')")) {
            ps.setString(1, reportedUUID.toString());
            ps.setString(2, reportedName);
            ps.setString(3, reporterUUID.toString());
            ps.setString(4, reporterName);
            ps.setInt(5, reasonId);
            ps.setString(6, reason);
            ps.setString(7, server);
            ps.setLong(8, timestamp);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Failed to insert report: {}", e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getActiveReports(int limit) {
        List<Map<String, Object>> reports = new ArrayList<>();
        if (connection == null) return reports;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT r.*, pr.name as reason_name, pr.description as reason_description FROM reports r LEFT JOIN punishment_reasons pr ON r.reason_id = pr.id WHERE r.status = 'OPEN' ORDER BY r.id DESC" + (limit > 0 ? " LIMIT " + limit : ""))) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> report = new java.util.HashMap<>();
                report.put("id", rs.getInt("id"));
                report.put("reported_uuid", rs.getString("reported_uuid"));
                report.put("reported_name", rs.getString("reported_name"));
                report.put("reported_by_uuid", rs.getString("reported_by_uuid"));
                report.put("reported_by_name", rs.getString("reported_by_name"));
                report.put("reason_id", rs.getInt("reason_id"));
                report.put("reason_name", rs.getString("reason_name"));
                report.put("reason_description", rs.getString("reason_description"));
                report.put("reason", rs.getString("reason"));
                report.put("server", rs.getString("server"));
                report.put("timestamp", rs.getLong("timestamp"));
                reports.add(report);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Failed to get active reports: {}", e.getMessage());
        }
        return reports;
    }

    public boolean handleReport(int reportId, UUID staffUUID, String staffName, String comment) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE reports SET status = 'CLOSED', resolved = TRUE, resolved_by = ?, resolved_by_name = ?, resolve_timestamp = ?, resolve_notes = ? WHERE id = ? AND status = 'OPEN'")) {
            ps.setString(1, staffUUID.toString());
            ps.setString(2, staffName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, comment);
            ps.setInt(5, reportId);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.error("Failed to handle report: {}", e.getMessage());
            return false;
        }
    }

    public void updateLastSeen(UUID uuid, long timestamp) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET last_seen = ? WHERE uuid = ?")) {
            ps.setLong(1, timestamp);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update last_seen for {}: {}", uuid, e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
