package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfigManager {

    private final BungeeSystem plugin;
    private Configuration config;
    private Configuration messages;
    private File configFile;
    private File messagesFile;

    public ConfigManager(BungeeSystem plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMessages();
    }

    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    plugin.getLogger().severe("Could not find config.yml inside the plugin jar!");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating config.yml: " + e.getMessage());
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error loading config.yml: " + e.getMessage());
        }
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream("messages.yml")) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                } else {
                    plugin.getLogger().severe("Could not find messages.yml inside the plugin jar!");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating messages.yml: " + e.getMessage());
            }
        }

        try {
            messages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error loading messages.yml: " + e.getMessage());
        }
    }

    public void reloadConfig() throws IOException {
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
    }

    public void reloadMessages() throws IOException {
        messages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(messagesFile);
    }

    public void reloadAll() throws IOException {
        reloadConfig();
        reloadMessages();
    }

    public Configuration getConfig() {
        return config;
    }

    public Configuration getMessages() {
        return messages;
    }

    public String getMessage(String path) {
        if (messages == null) {
            return "§cMessages configuration not loaded.";
        }
        
        String message = messages.getString(path);
        if (message == null) {
            return "§cMessage not found: " + path;
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        
        if (replacements.length % 2 != 0) {
            return message;
        }
        
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        
        return message;
    }

    public List<UUID> getMaintenanceWhitelist() {
        List<String> stringList = config.getStringList("maintenance.whitelist");
        List<UUID> uuidList = new ArrayList<>();
        
        for (String uuidStr : stringList) {
            try {
                uuidList.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in maintenance whitelist: " + uuidStr);
            }
        }
        
        return uuidList;
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving config: " + e.getMessage());
        }
    }

    public void addToWhitelist(UUID uuid) {
        List<String> whitelist = config.getStringList("maintenance.whitelist");
        if (!whitelist.contains(uuid.toString())) {
            whitelist.add(uuid.toString());
            config.set("maintenance.whitelist", whitelist);
            saveConfig();
        }
    }

    public void removeFromWhitelist(UUID uuid) {
        List<String> whitelist = config.getStringList("maintenance.whitelist");
        whitelist.remove(uuid.toString());
        config.set("maintenance.whitelist", whitelist);
        saveConfig();
    }

    public boolean isInWhitelist(UUID uuid) {
        List<String> whitelist = config.getStringList("maintenance.whitelist");
        return whitelist.contains(uuid.toString());
    }
} 