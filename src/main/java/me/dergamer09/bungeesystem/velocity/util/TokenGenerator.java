package me.dergamer09.bungeesystem.velocity.util;

import me.dergamer09.bungeesystem.velocity.VelocitySystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Utility class for generating and managing server tokens for Velocity
 */
public class TokenGenerator {
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Generate a cryptographically secure random token
     * 
     * @param prefix The prefix for the token (e.g., "BungeeSystem")
     * @return A secure random token
     */
    public static String generateToken(String prefix) {
        StringBuilder token = new StringBuilder();
        
        if (prefix != null && !prefix.isEmpty()) {
            token.append(prefix).append("_");
        }
        
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        
        return token.toString();
    }
    
    /**
     * Generate and save a token to token.yml for Velocity
     * 
     * @param plugin The VelocitySystem plugin instance
     * @return The generated token
     */
    public static String generateAndSaveToken(VelocitySystem plugin) {
        String token = generateToken("BungeeSystem");
        
        try {
            Path tokenFile = plugin.getDataDirectory().resolve("token.yml");
            
            // Create data folder if it doesn't exist
            Files.createDirectories(plugin.getDataDirectory());
            
            // Create or update token.yml
            StringBuilder content = new StringBuilder();
            content.append("# Server Token Configuration for BungeeSystem API\n");
            content.append("server:\n");
            content.append("  token: \"").append(token).append("\"\n");
            content.append("  api_url: \"http://194.15.36.81:3000\"\n");
            content.append("  \n");
            content.append("# API Configuration\n");
            content.append("api:\n");
            content.append("  enabled: true\n");
            content.append("  timeout: 5000  # milliseconds\n");
            content.append("  retry_attempts: 3\n");
            content.append("  \n");
            content.append("# Endpoints\n");
            content.append("endpoints:\n");
            content.append("  health: \"/health\"\n");
            content.append("  bans: \"/auth/punishments/bans\"\n");
            content.append("  mutes: \"/auth/punishments/mutes\"\n");
            content.append("  warns: \"/auth/punishments/warns\"\n");
            content.append("  kicks: \"/auth/punishments/kicks\"\n");
            content.append("  reports: \"/auth/reports\"\n");
            content.append("  player_events: \"/auth/players/events\"\n");
            content.append("  player_stats: \"/auth/players/stats\"\n");
            content.append("  server_status: \"/auth/servers/{server_id}/status\"\n");
            
            Files.write(tokenFile, content.toString().getBytes(StandardCharsets.UTF_8));
            
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
            
            String content = new String(Files.readAllBytes(tokenFile), StandardCharsets.UTF_8);
            return content.contains("token:") && !content.contains("your_server_token_here");
            
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Mask token for logging (show only first and last 4 characters)
     * 
     * @param token The token to mask
     * @return Masked token
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
     * @return true if token format is valid
     */
    public static boolean isValidTokenFormat(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        // Should start with "BungeeSystem_" and be at least 20 characters
        return token.startsWith("BungeeSystem_") && token.length() >= 20;
    }
}