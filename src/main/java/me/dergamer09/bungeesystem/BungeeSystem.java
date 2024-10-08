package me.dergamer09.bungeesystem;

import me.dergamer09.bungeesystem.commands.*;
import me.dergamer09.bungeesystem.listeners.PlayerEventListener;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.*;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;

public final class BungeeSystem extends Plugin {

    private final String prefix = ChatColor.DARK_GRAY + "| " + ChatColor.RED + "ᴍɪɴᴇᴄᴏꜱɪᴀ " + ChatColor.GRAY + "» ";
    private String webhookUrl;

    private final String currentVersion = "1.1.3-BETA";  // Deine aktuelle Version
    private final String jenkinsApiUrl = "https://ci.darkhex24.de/job/BungeeSystem/lastSuccessfulBuild/api/json";

    private Configuration config;
    private File configFile;

    private static BungeeSystem instance;
    private Connection connection;

    @Override
    public void onEnable() {
        // Registrierung der Befehle
        PluginManager pm = getProxy().getPluginManager();
        pm.registerCommand(this, new TeamChatCommand("teamchat", "bungeesystem.teamchat.use"));
        pm.registerCommand(this, new JoinMeCommand("joinme", "bungeesystem.joinme.use"));
        getProxy().getPluginManager().registerCommand(this, new LobbyCommand("l"));
        getProxy().getPluginManager().registerCommand(this, new LobbyCommand("lobby"));
        getProxy().getPluginManager().registerCommand(this, new LobbyCommand("hub"));
        getProxy().getPluginManager().registerCommand(this, new BlockBungeeCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ReportCommand());
        getProxy().getPluginManager().registerCommand(this, new ListCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ToggleNotifyCommand());

        // Registrierung des neuen OnlineTimeCommand
        OnlineTimeCommand onlineTimeCommand = new OnlineTimeCommand(this);
        getProxy().getPluginManager().registerCommand(this, onlineTimeCommand);

        webhookUrl = getConfig().getString("webhookUrl");
        getLogger().info("Discord Webhook URL: " + webhookUrl);

        getProxy().getPluginManager().registerListener(this, new PlayerEventListener(this));

        instance = this;
        loadConfig();
        connectToDatabase();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        getLogger().info(prefix + ChatColor.GRAY + "-------------------------------------");
        getLogger().info(prefix + ChatColor.GREEN + "Plugin wurde erfolgreich gestartet!");
        getLogger().info(prefix + ChatColor.DARK_AQUA + "Plugin by DerGamer09");
        getLogger().info(prefix + ChatColor.DARK_AQUA + "Version " + currentVersion);
        getLogger().info(prefix + ChatColor.GRAY + "-------------------------------------");

        checkForUpdates();
    }

    @Override
    public void onDisable() {
        closeDatabaseConnection();
    }

    private void checkForUpdates() {
        try {
            URL url = new URL(jenkinsApiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            // Basic Auth Header hinzufügen
            String userCredentials = "dergamer09:112f9f6cfeecac7c9f5a2ca09df155280e"; // Ersetze "deinBenutzername" und "deinApiToken"
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                connection.disconnect();

                String jsonResponse = content.toString();
                String latestVersion = extractVersionFromJson(jsonResponse);

                if (latestVersion != null) {
                    if (!currentVersion.equals(latestVersion)) {
                        getLogger().warning(ChatColor.YELLOW + "Ein neues Update ist verfügbar: " + latestVersion + " (aktuelle Version: " + currentVersion + ")");
                    } else if (isSameSnapshot(latestVersion)) {
                        getLogger().info(ChatColor.GREEN + "Dein Plugin ist auf dem neuesten Stand.");
                    } else {
                        getLogger().warning(ChatColor.YELLOW + "Es gibt ein neues Snapshot-Build: " + latestVersion + " (aktuelles Build: " + currentVersion + ")");
                    }
                } else {
                    getLogger().severe(ChatColor.RED + "Fehler beim Extrahieren der Versionsinformation.");
                }

            } else {
                getLogger().severe(ChatColor.RED + "Fehler beim Abrufen der Versionsinformation. HTTP Fehlercode: " + responseCode);
            }

        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "Fehler beim Überprüfen auf Updates: " + e.getMessage());
        }
    }

    private boolean isSameSnapshot(String latestVersion) {
        // Überprüft, ob beide Versionen denselben Snapshot-Namen haben (z.B. "1.2-SNAPSHOT")
        return currentVersion.equals(latestVersion);
    }

    private String extractVersionFromJson(String jsonResponse) {
        try {
            int index = jsonResponse.indexOf("\"displayName\":\"");
            if (index != -1) {
                int start = index + 14; // 14 = Länge von "displayName":"
                int end = jsonResponse.indexOf("\"", start);
                return jsonResponse.substring(start, end);
            }
        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "Fehler beim Extrahieren der Version aus der JSON-Antwort: " + e.getMessage());
        }
        return null;
    }

    // LobbyCommand Klasse für /l, /lobby und /hub
    public class LobbyCommand extends Command {
        public LobbyCommand(String name) {
            super(name);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(prefix + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;
            ServerInfo lobby = getProxy().getServerInfo("Lobby");

            if (lobby != null) {
                player.connect(lobby);
                player.sendMessage(prefix + ChatColor.GREEN + "Du wirst zur" + ChatColor.YELLOW + " Lobby" + ChatColor.GREEN + " teleportiert...");
            } else {
                player.sendMessage(prefix + ChatColor.RED + "Die" + ChatColor.YELLOW + " Lobby" + ChatColor.RED + " ist derzeit nicht verfügbar.");
            }
        }
    }

    // ReportCommand Klasse
    public class ReportCommand extends Command {
        public ReportCommand() {
            super("report");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(prefix + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;

            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.GRAY + "/report" + ChatColor.DARK_AQUA + " <Spieler>" + ChatColor.DARK_AQUA + " <Grund>");
                return;
            }

            String reportedPlayer = args[0];
            String reason = joinArray(args, 1, args.length);

            player.sendMessage(prefix + ChatColor.GREEN + "Danke für deinen "+ ChatColor.YELLOW + "Report! " + ChatColor.GREEN + "Wir werden den Fall prüfen.");

            // Nachricht an Discord Webhook senden
            sendReportToDiscord(player.getName(), reportedPlayer, reason);
        }

        private void sendReportToDiscord(String reporter, String reportedPlayer, String reason) {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonPayload = String.format(
                        "{\"content\": null, \"embeds\": [{\"title\": \"Neuer Report\",\"color\": 14177041,\"fields\": [" +
                                "{\"name\": \"Reporter\",\"value\": \"%s\",\"inline\": true}," +
                                "{\"name\": \"Gemeldeter Spieler\",\"value\": \"%s\",\"inline\": true}," +
                                "{\"name\": \"Grund\",\"value\": \"%s\",\"inline\": false}]}]}",
                        reporter, reportedPlayer, reason
                );

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.getBytes());
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Erfolg: Verbindung wird automatisch geschlossen
                } else {
                    // Fehlerbehandlung falls benötigt
                    System.err.println(prefix + ChatColor.RED + "Fehler beim Senden des Reports. HTTP Fehlercode: " + responseCode);
                }

                connection.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration getConfig() {
        return config;
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("prefix"));
    }

    public String getDefaultMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("defaultMessageColor"));
    }

    public String getUpdateMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("updateMessageColor"));
    }

    public String getSuccessMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("successMessageColor"));
    }

    public String getErrorMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("errorMessageColor"));
    }

    private String joinArray(String[] array, int start, int end) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(array[i]);
        }
        return result.toString();
    }

    public static BungeeSystem getInstance() {
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    // Verbindung zur MySQL-Datenbank herstellen
    private void connectToDatabase() {
        String host = config.getString("mysql.host");
        String port = config.getString("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        try {
            connection = DriverManager.getConnection(url, username, password);
            getLogger().info("MySQL connection established.");
        } catch (SQLException e) {
            getLogger().severe("MySQL connection failed: " + e.getMessage());
        }
    }

    private void closeDatabaseConnection() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("MySQL connection closed.");
            } catch (SQLException e) {
                getLogger().severe("Failed to close MySQL connection: " + e.getMessage());
            }
        }
    }

}
