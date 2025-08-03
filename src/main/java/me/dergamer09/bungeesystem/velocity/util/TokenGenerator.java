package me.dergamer09.bungeesystem.velocity.util;

import me.dergamer09.bungeesystem.velocity.VelocitySystem;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility class for generating and managing server tokens (Velocity version)
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
     * @param plugin The VelocitySystem plugin instance
     * @return The generated token
     */
    public static String generateAndSaveToken(VelocitySystem plugin) {
        String token = generateToken("VelocitySystem");
        
        try {
            Path dataFolder = plugin.getDataDirectory();
            Path tokenFile = dataFolder.resolve("token.yml");
            
            // Create data folder if it doesn't exist
            Files.createDirectories(dataFolder);
            
            // Create default token.yml if it doesn't exist
            if (!Files.exists(tokenFile)) {
                Files.createFile(tokenFile);
            }
            
            // Create default configuration content
            String defaultConfig = 
                "# Server Token Configuration for VelocitySystem API\n" +
                "# This token is used to authenticate with the dashboard API\n" +
                "\n" +
                "server:\n" +
                "  token: \"" + token + "\"\n" +
                "  api_url: \"http://api.devvoxel.net/\"\n" +
                "  \n" +
                "# API Configuration\n" +
                "api:\n" +
                "  enabled: true\n" +
                "  timeout: 5000  # milliseconds\n" +
                "  retry_attempts: 3\n" +
                "  \n" +
                "# Endpoints\n" +
                "endpoints:\n" +
                "  health: \"/auth/health\"\n" +
                "  bans: \"/auth/punishments/bans\"\n" +
                "  mutes: \"/auth/punishments/mutes\"\n" +
                "  reports: \"/auth/reports\"\n";
            
            // Write configuration to file
            Files.write(tokenFile, defaultConfig.getBytes());
            
            plugin.getLogger().info("Generated new server token: " + maskToken(token));
            plugin.getLogger().info("Token saved to token.yml");
            
            return token;
            
        } catch (IOException e) {
            plugin.getLogger().error("Failed to save token to token.yml: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Check if a token exists in token.yml
     * 
     * @param plugin The VelocitySystem plugin instance
     * @return true if a token exists, false otherwise
     */
    public static boolean tokenExists(VelocitySystem plugin) {
        try {
            Path tokenFile = plugin.getDataDirectory().resolve("token.yml");
            if (!Files.exists(tokenFile)) {
                return false;
            }
            
            String content = new String(Files.readAllBytes(tokenFile));
            return content.contains("token:") && 
                   !content.contains("your_server_token_here") &&
                   content.contains("VelocitySystem_");
            
        } catch (IOException e) {
            plugin.getLogger().warn("Failed to check token existence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the current token from token.yml
     * 
     * @param plugin The VelocitySystem plugin instance
     * @return The current token, or null if not found
     */
    public static String getCurrentToken(VelocitySystem plugin) {
        try {
            Path tokenFile = plugin.getDataDirectory().resolve("token.yml");
            if (!Files.exists(tokenFile)) {
                return null;
            }
            
            String content = new String(Files.readAllBytes(tokenFile));
            String[] lines = content.split("\n");
            
            for (String line : lines) {
                if (line.trim().startsWith("token:")) {
                    String token = line.substring(line.indexOf(":") + 1).trim();
                    token = token.replace("\"", "").replace("'", "");
                    
                    if (!token.isEmpty() && !token.equals("your_server_token_here")) {
                        return token;
                    }
                }
            }
            
            return null;
            
        } catch (IOException e) {
            plugin.getLogger().warn("Failed to get current token: " + e.getMessage());
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