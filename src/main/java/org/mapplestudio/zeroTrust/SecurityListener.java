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
                plugin.adventure().sender(player).sendMessage(Component.text("⛔ You are not authorized to use this command.", NamedTextColor.RED));
                
                String alert = "⚠️ **Command Blocked:** " + player.getName() + " tried to use `" + message + "`";
                plugin.getLogger().warning(alert);
                // Optional: Send webhook for attempted breaches? Maybe too spammy.
            }
        }
    }

    // --- 2. Anti-Nuker (Destruction Limiter) ---
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        // Trusted admins might be using WorldEdit or Creative, so maybe skip them? 
        // Requirement didn't specify, but usually admins are exempt. Let's exempt them.
        if (isTrusted(player)) return;

        UUID uuid = player.getUniqueId();
        int count = blocksBrokenPerSecond.getOrDefault(uuid, 0) + 1;
        blocksBrokenPerSecond.put(uuid, count);

        if (count > maxBlocksPerSecond) {
            event.setCancelled(true);
            // Warn only once per second to avoid spam
            if (count == maxBlocksPerSecond + 1) {
                plugin.adventure().sender(player).sendMessage(Component.text("⚠️ You are breaking blocks too fast!", NamedTextColor.RED));
            }
        }
    }

    // --- 3. Anti-Crash (Book Exploit) ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        if (event.getNewBookMeta().getPageCount() > 50) {
            event.setCancelled(true);
            event.getPlayer().kickPlayer("§cInvalid Book Data");
            plugin.getLogger().warning("Blocked potential book crash exploit from " + event.getPlayer().getName());
        }
        
        // Check for unusual characters (basic check)
        for (String page : event.getNewBookMeta().getPages()) {
            if (page.length() > 256 && page.chars().anyMatch(c -> c > 0xFF)) { // Simple heuristic
                 // This is a very basic check. Real exploits are more complex.
                 // But limiting page count is the most effective simple measure.
            }
        }
    }

    // --- 4. Syntax Blocking (Tab Complete) ---
    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        Player player = (Player) event.getSender();
        
        if (isTrusted(player)) return;

        String buffer = event.getBuffer();
        // Block "/ver <tab>", "/about <tab>", "/? <tab>" or just generic plugin discovery
        // Requirement: "Block the usage of Execute command: <tab> exploits"
        // Usually this refers to blocking tab completion for commands the player doesn't have access to,
        // or specifically blocking the colon syntax like "/plugin:command".
        
        if (buffer.contains(":") || buffer.startsWith("/")) {
             // This is a broad stroke. Spigot.yml already has 'tab-complete: 0' option.
             // But to implement it here:
             // If the buffer is just "/", we might want to limit what is shown.
             // But the requirement specifically mentions "Execute command: <tab>".
             // Let's block colon syntax which reveals plugin names.
             if (buffer.contains(":")) {
                 event.setCancelled(true);
             }
        }
    }
}
