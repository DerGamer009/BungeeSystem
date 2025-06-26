package me.dergamer09.bungeesystem.velocity.util;

import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Simple Discord webhook utility for Velocity.
 */
public class WebhookUtil {
    public static void sendWebhook(String webhookUrl, String message, Logger logger) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }
        new Thread(() -> {
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
                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                logger.error("Failed to send webhook message", e);
            }
        }).start();
    }
}
