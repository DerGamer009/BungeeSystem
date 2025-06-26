package me.dergamer09.bungeesystem.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import java.nio.file.Path;

import me.dergamer09.bungeesystem.velocity.Managers.VelocityCommandManager;
import me.dergamer09.bungeesystem.velocity.Managers.ConfigManager;
import me.dergamer09.bungeesystem.velocity.Managers.ListenerManager;
import me.dergamer09.bungeesystem.velocity.Runnables.OnlineTimeUpdater;

/**
 * Velocity entry point for BungeeSystem.
 */
@Plugin(id = "bungeesystem", name = "BungeeSystem", version = "1.2.1-BETA", authors = "DerGamer09")
public class VelocitySystem {

    private static final String VERSION = "1.2.1-BETA";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private VelocityCommandManager commandManager;
    private ListenerManager listenerManager;

    @Inject
    public VelocitySystem(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public ProxyServer getServer() { return server; }
    public String getVersion() { return VERSION; }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        logger.info("BungeeSystem loaded (Velocity compatibility mode).");
        configManager = new ConfigManager(logger, getClass().getClassLoader(), dataDirectory);
        commandManager = new VelocityCommandManager(this);
        listenerManager = new ListenerManager(this, logger);

        commandManager.registerCommands();
        listenerManager.registerListeners();

        // Schedule placeholder task
        server.getScheduler().buildTask(this, new OnlineTimeUpdater(this))
                .repeat(java.time.Duration.ofSeconds(1)).schedule();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("BungeeSystem disabled (Velocity compatibility mode).");
    }

    public ConfigManager getConfigManager() { return configManager; }
}
