package org.mapplestudio.zeroTrust;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityListener implements Listener {

    private final ZeroTrust plugin;
    private final Set<String> trustedAdmins;
    private final Set<String> blockedCommands;
    
    // Anti-Nuker State
    private final Map<UUID, Integer> blocksBrokenPerSecond = new ConcurrentHashMap<>();
    private final int maxBlocksPerSecond;

    public SecurityListener(ZeroTrust plugin) {
        this.plugin = plugin;
        this.trustedAdmins = new HashSet<>(plugin.getConfig().getStringList("trusted-admins"));
        
        List<String> cmds = plugin.getConfig().getStringList("blocked-commands");
        this.blockedCommands = new HashSet<>();
        for (String cmd : cmds) {
            this.blockedCommands.add(cmd.toLowerCase());
        }
        
        this.maxBlocksPerSecond = plugin.getConfig().getInt("max-blocks-per-second", 20);

        // Reset block counters every second
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, blocksBrokenPerSecond::clear, 20L, 20L);
    }

    private boolean isTrusted(Player player) {
        return trustedAdmins.contains(player.getName()) || trustedAdmins.contains(player.getUniqueId().toString());
    }

    // --- 1. Command Blocker ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();
        String command = message.split(" ")[0];

        if (blockedCommands.contains(command)) {
            if (!isTrusted(player)) {
                event.setCancelled(true);
                // Paper API supports Adventure components directly on Player
                player.sendMessage(Component.text("⛔ You are not authorized to use this command.", NamedTextColor.RED));
                
                String alert = "⚠️ **Command Blocked:** " + player.getName() + " tried to use `" + message + "`";
                plugin.getLogger().warning(alert);
            }
        }
    }

    // --- 2. Anti-Nuker (Destruction Limiter) ---
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isTrusted(player)) return;

        UUID uuid = player.getUniqueId();
        int count = blocksBrokenPerSecond.getOrDefault(uuid, 0) + 1;
        blocksBrokenPerSecond.put(uuid, count);

        if (count > maxBlocksPerSecond) {
            event.setCancelled(true);
            // Warn only once per second to avoid spam
            if (count == maxBlocksPerSecond + 1) {
                player.sendMessage(Component.text("⚠️ You are breaking blocks too fast!", NamedTextColor.RED));
            }
        }
    }

    // --- 3. Anti-Crash (Book Exploit) ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        if (event.getNewBookMeta().pageCount() > 50) { // Paper uses pageCount() instead of getPageCount() in some versions, but getPageCount() is standard Bukkit.
            // Actually, Paper API is backward compatible. getPageCount() is fine.
            // But let's check if we need to use Component for kick.
            event.setCancelled(true);
            event.getPlayer().kick(Component.text("Invalid Book Data", NamedTextColor.RED));
            plugin.getLogger().warning("Blocked potential book crash exploit from " + event.getPlayer().getName());
        }
    }

    // --- 4. Syntax Blocking (Tab Complete) ---
    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        Player player = (Player) event.getSender();
        
        if (isTrusted(player)) return;

        String buffer = event.getBuffer();
        if (buffer.contains(":") || buffer.startsWith("/")) {
             if (buffer.contains(":")) {
                 event.setCancelled(true);
             }
        }
    }
}
