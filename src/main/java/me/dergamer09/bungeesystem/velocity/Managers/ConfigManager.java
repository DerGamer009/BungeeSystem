package me.dergamer09.bungeesystem.velocity.Managers;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.slf4j.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simplified configuration manager for Velocity.
 * Loads config.yml and language-specific message files from the plugin's data directory.
 */
public class ConfigManager {

    private final Logger logger;
    private final ClassLoader classLoader;
    private final Path dataDirectory;

    private Configuration config;
    private Configuration messages;
    private File configFile;
    private File messagesFile;

    public ConfigManager(Logger logger, ClassLoader classLoader, Path dataDirectory) {
        this.logger = logger;
        this.classLoader = classLoader;
        this.dataDirectory = dataDirectory;
        loadConfig();
        loadMessages();
    }

    public void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            configFile = dataDirectory.resolve("config.yml").toFile();
            if (!configFile.exists()) {
                try (InputStream in = classLoader.getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile.toPath());
                    }
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            logger.error("Error loading config.yml", e);
        }
    }

    public void loadMessages() {
        String lang = config.getString("language", "en");
        messagesFile = dataDirectory.resolve("messages_" + lang + ".yml").toFile();
        if (!messagesFile.exists()) {
            String res = "messages_" + lang + ".yml";
            try (InputStream in = classLoader.getResourceAsStream(res)) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                }
            } catch (IOException e) {
                logger.error("Error saving " + res, e);
            }
        }
        try {
            messages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(messagesFile);
        } catch (IOException e) {
            logger.error("Error loading messages", e);
        }
    }

    public Configuration getConfig() {
        return config;
    }

    public void reloadAll() throws IOException {
        loadConfig();
        loadMessages();
    }

    public String getMessage(String path) {
        if (messages == null) {
            return "§cMessages configuration not loaded.";
        }
        String message = messages.getString(path);
        if (message == null) {
            return "§cMessage not found: " + path;
        }
        return translateColorCodes(message);
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

    public Component getMessageComponent(String path, String... replacements) {
        String msg = getMessage(path, replacements);
        return LegacyComponentSerializer.legacySection().deserialize(msg);
    }

    public List<UUID> getMaintenanceWhitelist() {
        List<String> list = config.getStringList("maintenance.whitelist");
        List<UUID> uuids = new ArrayList<>();
        for (String uuidStr : list) {
            try {
                uuids.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return uuids;
    }

    public static String translateColorCodes(String text) {
        return translateColorCodes('&', text);
    }

    public static String translateColorCodes(char altColorChar, String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == altColorChar && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (isColorCode(next)) {
                    builder.append('\u00A7').append(Character.toLowerCase(next));
                    i++;
                    continue;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private static boolean isColorCode(char c) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) >= 0;
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            logger.error("Error saving config", e);
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
