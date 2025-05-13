package me.dergamer09.bungeesystem.Managers;

import me.dergamer09.bungeesystem.BungeeSystem;
import net.md_5.bungee.api.ChatColor;

/**
 * Manages startup tasks and logging for the BungeeSystem plugin
 */
public class StartupManager {

    private final BungeeSystem plugin;
    private final String version;
    
    public StartupManager(BungeeSystem plugin, String version) {
        this.plugin = plugin;
        this.version = version;
    }
    
    /**
     * Displays startup messages and banner
     */
    public void displayStartupBanner() {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix"));
        
        plugin.getLogger().info(prefix + ChatColor.GRAY + "-------------------------------------");
        plugin.getLogger().info(prefix + ChatColor.GREEN + "Plugin wurde erfolgreich gestartet!");
        plugin.getLogger().info(prefix + ChatColor.DARK_AQUA + "Plugin by DerGamer09");
        plugin.getLogger().info(prefix + ChatColor.DARK_AQUA + "Version " + version);
        plugin.getLogger().info(prefix + ChatColor.GRAY + "-------------------------------------");
    }
    
    /**
     * Performs startup checks and validations
     * 
     * @return True if all checks pass, false otherwise
     */
    public boolean performStartupChecks() {
        // Check if configuration is loaded
        if (plugin.getConfig() == null) {
            plugin.getLogger().severe("Config file could not be loaded! Disabling plugin...");
            return false;
        }
        
        // Database connection is now checked separately in the main class
        
        // Check webhook URL is configured (optional)
        String webhookUrl = plugin.getConfig().getString("webhookUrl");
        if (webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.equals("https://your-discord-webhook-url.com")) {
            plugin.getLogger().info("Discord webhook configured: " + webhookUrl.replaceAll("https://discord.com/api/webhooks/\\d+/[^/]+", "https://discord.com/api/webhooks/REDACTED/REDACTED"));
        } else {
            plugin.getLogger().info("Discord webhook not configured or using default value. Webhook features will be disabled.");
        }
        
        // Ensure data directory exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        
        return true;
    }
} 