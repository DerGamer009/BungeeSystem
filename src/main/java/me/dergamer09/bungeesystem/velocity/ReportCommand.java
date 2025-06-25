package me.dergamer09.bungeesystem.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Simple /report command for Velocity that sends a message to the configured Discord webhook.
 */
public class ReportCommand implements SimpleCommand {

    private final ProxyServer server;
    private final String webhookUrl;

    public ReportCommand(ProxyServer server, String webhookUrl) {
        this.server = server;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Only players can use this command."));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: /report <player> <reason> [details]"));
            return;
        }

        Player reporter = (Player) source;
        String target = args[0];
        String reason = args[1];
        String details = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        String serverName = reporter.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("Unknown");

        String content = "**Neuer Report**\n" +
                "Spieler: " + target + "\n" +
                "Reporter: " + reporter.getUsername() + "\n" +
                "Grund: " + reason + (details.isEmpty() ? "" : " - " + details) + "\n" +
                "Server: " + serverName;

        sendWebhook(content);
        source.sendMessage(Component.text("Report gesendet."));
    }

    private void sendWebhook(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("your-discord-webhook-url")) {
            return;
        }
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = "{\"content\":\"" + message.replace("\"", "\\\"") + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            server.getConsoleCommandSource().sendMessage(Component.text("Webhook Error: " + e.getMessage()));
        }
    }
}
