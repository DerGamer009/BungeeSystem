package me.dergamer09.bungeesystem.util;

import me.dergamer09.bungeesystem.BungeeSystem;
import org.json.simple.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for sending simple Discord webhook messages.
 */
public class WebhookUtil {

    /**
     * Send a plain text message to a Discord webhook.
     *
     * @param webhookUrl The webhook URL
     * @param message    The message content
     * @param plugin     Plugin instance for logging
     */
    public static void sendWebhook(String webhookUrl, String message, BungeeSystem plugin) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        // Run the network request asynchronously to avoid blocking the main thread
        net.md_5.bungee.api.ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("content", message);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.toJSONString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_NO_CONTENT && responseCode != HttpURLConnection.HTTP_OK) {
                    plugin.getLogger().warning("Discord webhook responded with code " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send webhook message: " + e.getMessage());
            }
        });
    }
}
