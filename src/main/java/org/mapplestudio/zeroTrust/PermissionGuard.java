package org.mapplestudio.zeroTrust;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionGuard extends BukkitRunnable {

    private final ZeroTrust plugin;
    private final Set<String> trustedAdmins;

    public PermissionGuard(ZeroTrust plugin) {
        this.plugin = plugin;
        this.trustedAdmins = new HashSet<>(plugin.getConfig().getStringList("trusted-admins"));
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isTrusted = trustedAdmins.contains(player.getName()) || trustedAdmins.contains(player.getUniqueId().toString());

            // Check for OP or Wildcard permission
            if (player.isOp() || player.hasPermission("*")) {
                if (!isTrusted) {
                    // 1. De-op immediately
                    player.setOp(false);
                    
                    // 2. Log & Webhook
                    String alert = "🚨 **SECURITY ALERT:** Unauthorized Admin Detected!\n" +
                            "**Player:** " + player.getName() + "\n" +
                            "**Action:** De-opped and Kicked.";
                    plugin.getLogger().warning(alert);
                    plugin.getWebhookSender().sendAlert(alert);

                    // 3. Kick
                    // Run on next tick to ensure de-op processes first and to avoid concurrent modification issues if any
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.kickPlayer("§c§lSecurity Violation\n§7You are not a trusted administrator.");
                    });
                }
            }
        }
    }
}
