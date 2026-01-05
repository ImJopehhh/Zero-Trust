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

        // 5. Register Command
        getCommand("zt").setExecutor(new ZeroTrustCommand(this));

        getLogger().info("Zero Trust - Runtime Protection Active.");
    }

    @Override
    public void onDisable() {
        // 1. Self-Defense with Webhook (The "Dead Man's Trigger")
        
        boolean isServerStopping = false;
        
        // Method 1: Check Paper API
        try {
            if (getServer().isStopping()) {
                isServerStopping = true;
            }
        } catch (NoSuchMethodError e) {
            // Ignore
        }

        // Method 2: Check Thread Name (Standard Spigot/Paper behavior)
        // When stopping, the thread is usually "Server Shutdown Thread" or similar.
        if (!isServerStopping) {
             String threadName = Thread.currentThread().getName().toLowerCase();
             if (threadName.contains("shutdown") || threadName.contains("stop")) {
                 isServerStopping = true;
             }
        }

        if (!isServerStopping) {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().toLowerCase().contains("shutdown")) {
                    isServerStopping = true;
                    break;
                }
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
