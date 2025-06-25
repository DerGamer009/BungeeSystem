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
    private String webhookUrl = "";

    @Inject
    public VelocitySystem(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        webhookUrl = loadWebhook();
        logger.info("BungeeSystem loaded (Velocity compatibility mode).");
        // Register commands
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("bsversion").plugin(this).build(),
                new VersionCommand(VERSION)
        );
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("report").plugin(this).build(),
                new ReportCommand(server, webhookUrl)
        );
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("BungeeSystem disabled (Velocity compatibility mode).");
    }

    private String loadWebhook() {
        java.nio.file.Path path = java.nio.file.Paths.get("plugins", "BungeeSystem", "config.yml");
        if (!java.nio.file.Files.exists(path)) {
            return "";
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("webhookUrl:\\s*\"?(.*?)\"?$");
        try {
            for (String line : java.nio.file.Files.readAllLines(path)) {
                java.util.regex.Matcher m = p.matcher(line.trim());
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to read webhookUrl: {}", e.getMessage());
        }
        return "";
    }
}
