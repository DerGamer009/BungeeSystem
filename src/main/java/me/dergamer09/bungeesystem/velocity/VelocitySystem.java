package me.dergamer09.bungeesystem.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

/**
 * Minimal Velocity entry point for BungeeSystem.
 * It currently only logs a startup message, but allows the
 * plugin jar to be loaded on Velocity proxies.
 */
@Plugin(id = "bungeesystem", name = "BungeeSystem", version = "1.2.0-SNAPSHOT")
public class VelocitySystem {

    private static final String VERSION = "1.2.0-SNAPSHOT";

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public VelocitySystem(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        logger.info("BungeeSystem loaded (Velocity compatibility mode).");
        // Register a simple version command
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("bsversion").plugin(this).build(),
                new VersionCommand(VERSION)
        );
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("BungeeSystem disabled (Velocity compatibility mode).");
    }
}
