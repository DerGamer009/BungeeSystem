package me.dergamer09.bungeesystem.Managers;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.config.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";

        try {
            connection = DriverManager.getConnection(url, username, password);
            ProxyServer.getInstance().getLogger().info("§a[BungeeSystem] MySQL connection established.");
        } catch (SQLException e) {
            ProxyServer.getInstance().getLogger().severe("§c[BungeeSystem] MySQL connection failed: " + e.getMessage());
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
                        "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                        "banned_by VARCHAR(36) NOT NULL," +
                        "reason_id INT NOT NULL," +
                        "timestamp BIGINT NOT NULL," +
                        "active BOOLEAN DEFAULT TRUE)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createMutesTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS mutes (" +
                        "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                        "muted_by VARCHAR(36) NOT NULL," +
                        "reason_id INT NOT NULL," +
                        "timestamp BIGINT NOT NULL," +
                        "active BOOLEAN DEFAULT TRUE)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createReportsTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS reports (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "reported_uuid VARCHAR(36) NOT NULL," +
                        "reported_by VARCHAR(36) NOT NULL," +
                        "reason_id INT NOT NULL," +
                        "timestamp BIGINT NOT NULL)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createOnlineTimeTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS online_time (" +
                        "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                        "total_time BIGINT NOT NULL DEFAULT 0," +
                        "last_login BIGINT DEFAULT 0)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}