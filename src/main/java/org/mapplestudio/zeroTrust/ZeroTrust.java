package org.mapplestudio.zeroTrust;

import org.bukkit.plugin.java.JavaPlugin;

public final class ZeroTrust extends JavaPlugin {

    private static ZeroTrust instance;
    private WebhookSender webhookSender;

    public static ZeroTrust getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // 1. Load Config
        saveDefaultConfig();

        // 2. Initialize Webhook Sender
        this.webhookSender = new WebhookSender(this);

        // 3. Register Listeners (Anti-Abuse, Exploit Prevention)
        getServer().getPluginManager().registerEvents(new SecurityListener(this), this);

        // 4. Start Permission Guard (The Task)
        // Runs every 100 ticks (5 seconds)
        new PermissionGuard(this).runTaskTimer(this, 100L, 100L);

        getLogger().info("Zero Trust - Runtime Protection Active.");
    }

    @Override
    public void onDisable() {
        // 1. Self-Defense with Webhook (The "Dead Man's Trigger")
        // Paper API provides getServer().isStopping()
        
        boolean isServerStopping = false;
        try {
            if (getServer().isStopping()) {
                isServerStopping = true;
            }
        } catch (NoSuchMethodError e) {
            // Fallback for older versions or if method missing (unlikely in Paper 1.21)
            if (Thread.currentThread().getName().toLowerCase().contains("shutdown")) {
                isServerStopping = true;
            }
        }

        if (!isServerStopping) {
            String alert = "🚨 **SECURITY ALERT:** Zero Trust was forcibly disabled by a plugin or user while the server is running!";
            getLogger().severe(alert);
            webhookSender.sendAlert(alert);
        }

        getLogger().info("Zero Trust Deactivated.");
    }

    public WebhookSender getWebhookSender() {
        return webhookSender;
    }
}
