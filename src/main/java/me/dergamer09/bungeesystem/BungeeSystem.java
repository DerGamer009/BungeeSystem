package me.dergamer09.bungeesystem;

import me.dergamer09.bungeesystem.Managers.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.plugin.Plugin;

/**
 * Main plugin class for BungeeSystem
 */
public final class BungeeSystem extends Plugin {

    private static BungeeSystem instance;
    
    // Version information
    private final String currentVersion = "1.2.2-SNAPSHOT";
    
    // Management systems
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private ListenerManager listenerManager;
    private UpdateManager updateManager;
    private StartupManager startupManager;
    private StatsManager statsManager;
    private ChatManager chatManager;
    private PunishmentManager punishmentManager;
    
    // Plugin data
    private Configuration config;
    private String webhookUrl;
    
    // Messaging system maps
    public static final Map<UUID, UUID> lastMessageMap = new HashMap<>();
    public static final Map<UUID, Set<UUID>> ignoredPlayers = new HashMap<>();

    @Override
    public void onEnable() {
        // Set static instance
        instance = this;
        
        // Initialize managers
        initializeManagers();
        
        // Initialize database with detailed diagnostics and retry
        if (!initializeDatabase()) {
            getLogger().severe("Database initialization failed. Plugin cannot function without database access. Disabling...");
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getPluginManager().unregisterCommands(this);
            return;
        }
        
        // Perform startup checks
        if (!startupManager.performStartupChecks()) {
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getPluginManager().unregisterCommands(this);
            return;
        }
        
        // Set up stats tables and initialize stats-related features
        statsManager.setupTables();
        
        // Start the punishment cleanup task
        punishmentManager.scheduleCleanupTask();
        
        // Register commands and listeners
        commandManager.registerCommands();
        listenerManager.registerListeners();
        listenerManager.scheduleRecurringTasks();
        
        // Display startup banner
        startupManager.displayStartupBanner();
        
        // Check for updates
        updateManager.checkForUpdates();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info(ChatColor.RED + "BungeeSystem has been disabled.");
    }
    
    /**
     * Initialize plugin managers
     */
    private void initializeManagers() {
        // Config manager must be first so other managers can access configuration
        configManager = new ConfigManager(this);
        config = configManager.getConfig();
        
        // Initialize managers that don't need database connectivity
        databaseManager = new DatabaseManager(getConfig());
        commandManager = new CommandManager(this);
        listenerManager = new ListenerManager(this);
        updateManager = new UpdateManager(this, currentVersion);
        startupManager = new StartupManager(this, currentVersion);
        chatManager = new ChatManager(this);
        
        // Set webhook URL from config
        webhookUrl = config.getString("webhookUrl", "");
        
        // Database-dependent managers will be initialized after database connection is established
    }

    /**
     * Get the plugin's configuration
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Get the plugin prefix with color codes translated
     */
    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("prefix"));
    }

    /**
     * Get the default message color
     */
    public String getDefaultMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("defaultMessageColor"));
    }

    /**
     * Get the update message color
     */
    public String getUpdateMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("updateMessageColor"));
    }

    /**
     * Get the success message color
     */
    public String getSuccessMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("successMessageColor"));
    }

    /**
     * Get the error message color
     */
    public String getErrorMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("errorMessageColor"));
    }

    /**
     * Get the plugin instance
     */
    public static BungeeSystem getInstance() {
        return instance;
    }

    /**
     * Get the database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Get the config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    /**
     * Get the listener manager
     */
    public ListenerManager getListenerManager() {
        return listenerManager;
    }
    
    /**
     * Get the update manager
     */
    public UpdateManager getUpdateManager() {
        return updateManager;
    }
    
    /**
     * Get the stats manager
     */
    public StatsManager getStatsManager() {
        return statsManager;
    }
    
    /**
     * Get the chat manager
     */
    public ChatManager getChatManager() {
        return chatManager;
    }
    
    /**
     * Get the punishment manager
     */
    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }
    
    /**
     * Get the webhook URL
     */
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    /**
     * Initialize the database connection with detailed diagnostics and retry logic
     * 
     * @return true if database was successfully initialized
     */
    private boolean initializeDatabase() {
        getLogger().info("Initializing database connection...");
        
        // Try to initialize with our enhanced method that includes database creation
        boolean success = databaseManager.initialize();
        
        if (!success) {
            getLogger().warning("Initial database connection failed. Running diagnostic tests...");
            databaseManager.testConnection();
            return false;
        }
        
        // Set up database tables
        getLogger().info("Setting up database tables...");
        databaseManager.setupTables();
        
        // Now that database is initialized, create database-dependent managers
        getLogger().info("Initializing database-dependent managers...");
        statsManager = new StatsManager(this);
        punishmentManager = new PunishmentManager(this);
        
        return true;
    }
}
