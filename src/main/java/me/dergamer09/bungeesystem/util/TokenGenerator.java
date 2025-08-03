package me.dergamer09.bungeesystem.util;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility class for generating and managing server tokens
 */
public class TokenGenerator {
    
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 32;
    private static final Random RANDOM = new SecureRandom();
    
    /**
     * Generate a random token
     * 
     * @return A random token string
     */
    public static String generateToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return token.toString();
    }
    
    /**
     * Generate a token with prefix
     * 
     * @param prefix The prefix to add to the token
     * @return A token with the specified prefix
     */
    public static String generateToken(String prefix) {
        return prefix + "_" + generateToken();
    }
    
    /**
     * Generate and save a token to token.yml
     * 
     * @param plugin The BungeeSystem plugin instance
     * @return The generated token
     */
    public static String generateAndSaveToken(BungeeSystem plugin) {
        String token = generateToken("BungeeSystem");
        
        try {
            File tokenFile = new File(plugin.getDataFolder(), "token.yml");
            
            // Create data folder if it doesn't exist
            plugin.getDataFolder().mkdirs();
            
            // Create default token.yml if it doesn't exist
            if (!tokenFile.exists()) {
                tokenFile.createNewFile();
            }
            
            // Load existing configuration
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(tokenFile);
            
            // Update the token
            config.set("server.token", token);
            config.set("server.api_url", "http://api.devvoxel.net/");
            config.set("api.enabled", true);
            config.set("api.timeout", 5000);
            config.set("api.retry_attempts", 3);
            config.set("endpoints.health", "/auth/health");
            config.set("endpoints.bans", "/auth/punishments/bans");
            config.set("endpoints.mutes", "/auth/punishments/mutes");
            config.set("endpoints.reports", "/auth/reports");
            
            // Save the configuration
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, tokenFile);
            
            plugin.getLogger().info("Generated new server token: " + maskToken(token));
            plugin.getLogger().info("Token saved to token.yml");
            
            return token;
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save token to token.yml: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Check if a token exists in token.yml
     * 
     * @param plugin The BungeeSystem plugin instance
     * @return true if a token exists, false otherwise
     */
    public static boolean tokenExists(BungeeSystem plugin) {
        try {
            File tokenFile = new File(plugin.getDataFolder(), "token.yml");
            if (!tokenFile.exists()) {
                return false;
            }
            
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(tokenFile);
            String token = config.getString("server.token", "");
            
            return !token.isEmpty() && !token.equals("your_server_token_here");
            
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to check token existence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the current token from token.yml
     * 
     * @param plugin The BungeeSystem plugin instance
     * @return The current token, or null if not found
     */
    public static String getCurrentToken(BungeeSystem plugin) {
        try {
            File tokenFile = new File(plugin.getDataFolder(), "token.yml");
            if (!tokenFile.exists()) {
                return null;
            }
            
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(tokenFile);
            String token = config.getString("server.token", "");
            
            if (token.isEmpty() || token.equals("your_server_token_here")) {
                return null;
            }
            
            return token;
            
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to get current token: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Mask token for logging (shows only first 4 and last 4 characters)
     * 
     * @param token The token to mask
     * @return The masked token
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
    
    /**
     * Validate token format
     * 
     * @param token The token to validate
     * @return true if the token is valid, false otherwise
     */
    public static boolean isValidToken(String token) {
        if (token == null || token.length() < 16) {
            return false;
        }
        
        // Check if token contains only valid characters
        for (char c : token.toCharArray()) {
            if (CHARS.indexOf(c) == -1) {
                return false;
            }
        }
        
        return true;
    }
} 