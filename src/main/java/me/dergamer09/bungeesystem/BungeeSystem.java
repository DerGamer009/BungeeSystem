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
import net.md_5.bungee.command.PlayerCommand;
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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;

public final class BungeeSystem extends Plugin {

    private final String prefix = "&8| &cBungeeSystem &7» ";
    private String webhookUrl;

    private final String currentVersion = "1.1.6";  // Deine aktuelle Version

    private Configuration config;
    private File configFile;

    private static BungeeSystem instance;
    private Connection connection;

    @Override
    public void onEnable() {

        // Ensure the configuration is loaded before accessing it
        loadConfig();

        if (getConfig() == null) {
            getLogger().severe("Config file could not be loaded! Disabling plugin...");
            return;
        }

        // Registrierung der Befehle
        PluginManager pm = getProxy().getPluginManager();
        pm.registerCommand(this, new TeamChatCommand());
        pm.registerCommand(this, new JoinMeCommand("joinme", "bungeesystem.joinme.use"));
        pm.registerCommand(this, new PingCommand());
        pm.registerCommand(this, new ServerListCommand());
        pm.registerCommand(this, new FindCommand());
        pm.registerCommand(this, new VanishCommand());
        pm.registerCommand(this, new PlayCommand());
        pm.registerCommand(this, new ServerCommand());
        pm.registerCommand(this, new UptimeCommand());
        pm.registerCommand(this, new SendCommand());
        pm.registerCommand(this, new BroadcastCommand());
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

    // Update Check from SpigotMC
    private void checkForUpdates() {
        try {
            URL url = new URL("https://api.spigotmc.org/simple/0.2/index.php?action=getResource&id=119339");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

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

                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(content.toString());
                String latestVersion = (String) jsonResponse.get("current_version");

                if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                    getLogger().warning(ChatColor.YELLOW + "A new update is available: " + latestVersion + " (Current Version: " + currentVersion + ")");
                } else {
                    getLogger().info(ChatColor.GREEN + "Your plugin is up to date.");
                }
            } else {
                getLogger().severe(ChatColor.RED + "Error retrieving version information from SpigotMC. HTTP Error Code: " + responseCode);
            }
        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "Error checking for updates via SpigotMC: " + e.getMessage());
        }
    }

    private boolean isSameSnapshot(String latestVersion) {
        return currentVersion.replaceAll("-SNAPSHOT", "").equals(latestVersion.replaceAll("-SNAPSHOT", ""));
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

        configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    getLogger().severe("Could not find config.yml inside the plugin jar!");
                }
            } catch (IOException e) {
                getLogger().severe("Error creating config.yml: " + e.getMessage());
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            getLogger().severe("Error loading config.yml: " + e.getMessage());
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
