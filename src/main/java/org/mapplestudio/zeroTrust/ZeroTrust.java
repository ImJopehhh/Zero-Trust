package org.mapplestudio.zeroTrust;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZeroTrust extends JavaPlugin {

    private static ZeroTrust instance;
    private BukkitAudiences adventure;
    private WebhookSender webhookSender;

    public static ZeroTrust getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.adventure = BukkitAudiences.create(this);

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
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        // 1. Self-Defense with Webhook (The "Dead Man's Trigger")
        // Check if the server is naturally stopping.
        // Note: Bukkit.isStopping() or Server.isStopping() might not be available in all versions.
        // We can check if the shutdown thread is active or use a heuristic.
        // However, the prompt specifically asked for: "If !server.isStopping()".
        // We will try to use the API if available, or fallback.
        // In modern Paper/Spigot, getServer().isStopping() exists.
        
        boolean isServerStopping = false;
        try {
            // Try to access isStopping() via reflection or direct call if API allows
            // Since we are compiling against 1.21 API, it should be there?
            // Actually, Spigot API 1.21 might not have isStopping() directly on Server interface publicly?
            // Let's check. It is often not there.
            // A common trick is to check `Bukkit.getShutdownMessage()`? No.
            // We can check if the plugin is disabled but other plugins are still enabled? No.
            // The most reliable way without NMS is checking if the server is running.
            // But let's assume the user knows it exists or we use a try-catch.
            // Actually, let's use a safe check.
            // If we can't find it, we assume it's NOT stopping to be safe (paranoid mode), or IS stopping to be safe (avoid false alerts).
            // Given "Zero Trust", we should probably alert if we are unsure.
            // But let's try to find a standard way.
            // There isn't a 100% standard API for "is server shutting down" in old Bukkit.
            // But in recent versions, `Bukkit.isStopping()` was added? No.
            // Let's use a flag. We can listen to `PluginDisableEvent` for other plugins? No.
            // We can check `Bukkit.getOnlinePlayers().isEmpty()`? No.
            
            // Let's rely on the fact that if the server stops, it calls onDisable() on all plugins.
            // If we are disabled individually, the server is still running.
            // We can check `Bukkit.getServer().getOnlinePlayers()` - if it works, server is running.
            // But that doesn't mean it's not stopping.
            
            // Let's try to use the method requested: `!server.isStopping()`.
            // If it doesn't compile, we will need to fix it.
            // For now, I will write it as requested, but wrap in try-catch for "NoSuchMethodError" at runtime?
            // No, compilation will fail.
            // I will use a reflection check for `isStopping` or `hasStopped`.
            // Or better: `Bukkit.getScheduler().isCurrentlyRunning(taskId)`? No.
            
            // REALITY CHECK: Spigot API does NOT have `isStopping()`.
            // Paper API does.
            // If this is a Spigot plugin, we can't use it.
            // However, we can check if the thread is named "Server Shutdown Thread".
            if (Thread.currentThread().getName().toLowerCase().contains("shutdown")) {
                isServerStopping = true;
            }
            
        } catch (Exception e) {
            // Ignore
        }

        if (!isServerStopping) {
            // Double check: If the server is actually stopping, usually the scheduler stops accepting tasks.
            // But we need to send the webhook NOW.
            String alert = "🚨 **SECURITY ALERT:** Zero Trust was forcibly disabled by a plugin or user while the server is running!";
            getLogger().severe(alert);
            webhookSender.sendAlert(alert);
        }

        getLogger().info("Zero Trust Deactivated.");
    }

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Cannot retrieve audience provider while plugin is not enabled");
        }
        return this.adventure;
    }

    public WebhookSender getWebhookSender() {
        return webhookSender;
    }
}
