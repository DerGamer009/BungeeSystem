package me.dergamer09.bungeesystem;

import me.dergamer09.bungeesystem.Managers.DatabaseManager;
import me.dergamer09.bungeesystem.Runnables.OnlineTimeUpdater;
import me.dergamer09.bungeesystem.commands.*;
import me.dergamer09.bungeesystem.listeners.MotdListener;
import me.dergamer09.bungeesystem.listeners.PlayerEventListener;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.*;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import net.md_5.bungee.api.plugin.Plugin;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class BungeeSystem extends Plugin {

    private final String prefix = "&8| &cBungeeSystem &7» ";
    private String webhookUrl;

    public static final Map<UUID, UUID> lastMessageMap = new HashMap<>();
    public static final Map<UUID, Set<UUID>> ignoredPlayers = new HashMap<>();

    private final String currentVersion = "1.1.8-SNAPSHOT";  // Deine aktuelle Version

    private Configuration config;
    private File configFile;

    private static BungeeSystem instance;

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {

        // Ensure the configuration is loaded before accessing it
        loadConfig();

        if (getConfig() == null) {
            getLogger().severe("Config file could not be loaded! Disabling plugin...");
            return;
        }

        databaseManager = new DatabaseManager(getConfig());
        databaseManager.connect();
        databaseManager.setupTables();

        if (databaseManager.getConnection() == null) {
            getLogger().severe("Database connection could not be established! Disabling plugin...");
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
        pm.registerCommand(this, new MaintenanceCommand());
        pm.registerListener(this, new MotdListener());
        pm.registerCommand(this, new MSGCommand());
        pm.registerCommand(this, new ReplyCommand());
        pm.registerCommand(this, new IgnoreCommand());
        pm.registerCommand(this, new BlockBungeeCommand(this));
        pm.registerCommand(this, new ListCommand(this));
        pm.registerCommand(this, new ToggleNotifyCommand());
        pm.registerCommand(this, new OnlineTimeCommand());
        pm.registerCommand(this, new LobbyCommand("lobby"));
        pm.registerCommand(this, new LobbyCommand("hub"));
        pm.registerCommand(this, new LobbyCommand("l"));

        pm.registerListener(this, new PlayerEventListener());

        getProxy().getScheduler().schedule(this, new OnlineTimeUpdater(), 1L, 1L, TimeUnit.SECONDS);

        webhookUrl = getConfig().getString("webhookUrl");
        getLogger().info("Discord Webhook URL: " + webhookUrl);

        instance = this;

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
        if (databaseManager != null) {
            databaseManager.close();
        }
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

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

}
