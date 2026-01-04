package org.mapplestudio.zeroTrust;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class WebhookSender {

    private final ZeroTrust plugin;
    private final String webhookUrl;

    public WebhookSender(ZeroTrust plugin) {
        this.plugin = plugin;
        this.webhookUrl = plugin.getConfig().getString("discord-webhook-url", "");
    }

    public void sendAlert(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("INSERT_URL_HERE")) {
            return;
        }

        // Run async to avoid blocking the main thread
        // Note: If called during onDisable, we might need to run on the current thread if the scheduler is shutting down.
        // However, for "Dead Man's Trigger", we usually want it to fire immediately.
        // We'll check if the plugin is enabled to decide whether to use scheduler or direct execution.
        
        Runnable task = () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JsonObject json = new JsonObject();
                json.addProperty("content", message);

                String jsonString = json.toString();
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Failed to send webhook. Response Code: " + responseCode);
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error sending webhook alert", e);
            }
        };

        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            new Thread(task).start();
        }
    }
}
