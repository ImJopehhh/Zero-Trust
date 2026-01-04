package org.mapplestudio.zeroTrust;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
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

            if (player.isOp() || player.hasPermission("*")) {
                if (!isTrusted) {
                    player.setOp(false);

                    String alert = "🚨 **SECURITY ALERT:** Unauthorized Admin Detected!\n" +
                            "**Player:** " + player.getName() + "\n" +
                            "**Action:** De-opped and Kicked.";
                    plugin.getLogger().warning(alert);
                    plugin.getWebhookSender().sendAlert(alert);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Paper API allows kicking with Component
                        player.kick(Component.text("Security Violation\nYou are not a trusted administrator.", NamedTextColor.RED));
                    });
                }
            }
        }
    }
}
