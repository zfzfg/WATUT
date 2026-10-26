package com.corosus.watut.paper;

import com.corosus.watut.common.WatutPlayerStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Event listener providing Bukkit-side hooks that replace the mod's server-side Mixins.
 */
public class WatutEventListener implements Listener {

    private final WatutPaperPlugin plugin;

    public WatutEventListener(WatutPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getStatusManager().getStatus(player.getUniqueId());

        // Update notice for admins upon join
        if (player.hasPermission("watut.admin") && plugin.getUpdateChecker() != null) {
            if (plugin.getUpdateChecker().isUpdateAvailable()) {
                player.sendMessage("§e[WATUT] §7A new version is available: §a"
                        + plugin.getUpdateChecker().getLatestVersion()
                        + " §7(You are on §c" + plugin.getUpdateChecker().getCurrentVersion() + "§7)");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getStatusManager().removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Reset idle tick counter if the player actively moved position
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            WatutPlayerStatus status = plugin.getStatusManager().getStatus(event.getPlayer().getUniqueId());
            status.setIdleTicks(0);
            if (status.isIdle()) {
                status.setIdle(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            WatutPlayerStatus status = plugin.getStatusManager().getStatus(player.getUniqueId());
            status.setIdleTicks(0);
            // Foundation for item movement animation sync to nearby players
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        WatutPlayerStatus status = plugin.getStatusManager().getStatus(event.getPlayer().getUniqueId());
        status.setIdleTicks(0);
    }
}
