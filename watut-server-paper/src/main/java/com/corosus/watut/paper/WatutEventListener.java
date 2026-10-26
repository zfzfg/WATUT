package com.corosus.watut.paper;

import com.corosus.watut.common.WatutChannels;
import com.corosus.watut.common.WatutPlayerStatus;
import com.corosus.watut.common.nbt.NbtCompound;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

/**
 * Event listener providing Bukkit-side hooks that replicate the mod's server-side tracking.
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

        // Sync initial state and server configs
        plugin.getStatusManager().syncAllToPlayer(player);

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
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        WatutPlayerStatus status = plugin.getStatusManager().getStatus(player.getUniqueId());
        status.setIdleTicks(0);
        if (status.isIdle()) {
            status.setIdle(false);
        }

        if (plugin.getConfig().getBoolean("dontSendDetailedItemInfo", false)) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            item = event.getCursor();
        }
        if (item == null || item.getType().isAir()) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() instanceof Container container) {
            Block block = container.getBlock();
            Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
            Location playerLoc = player.getLocation().add(0, 1.2, 0);

            boolean playerToContainer = event.getClickedInventory() != topInventory;
            Location from = playerToContainer ? playerLoc : blockLoc;
            Location to = playerToContainer ? blockLoc : playerLoc;

            try {
                NbtCompound movePacket = new NbtCompound();
                NbtCompound itemNbt = new NbtCompound();
                itemNbt.putString("id", item.getType().getKey().toString());
                itemNbt.putByte("Count", (byte) item.getAmount());

                movePacket.putCompound(WatutChannels.NBT_ITEM_TRANSFER_ITEMSTACK, itemNbt);
                movePacket.putFloat(WatutChannels.NBT_ITEM_TRANSFER_FROM_X, (float) from.getX());
                movePacket.putFloat(WatutChannels.NBT_ITEM_TRANSFER_FROM_Y, (float) from.getY());
                movePacket.putFloat(WatutChannels.NBT_ITEM_TRANSFER_FROM_Z, (float) from.getZ());
                movePacket.putFloat(WatutChannels.NBT_ITEM_TRANSFER_TO_X, (float) to.getX());
                movePacket.putFloat(WatutChannels.NBT_ITEM_TRANSFER_TO_Y, (float) to.getY());
                movePacket.putFloat(WatutChannels.NBT_ITEM_TRANSFER_TO_Z, (float) to.getZ());

                double maxDist = plugin.getConfig().getDouble("distanceRequiredToShowGUIInfo", 16.0);
                plugin.getStatusManager().sendToNearby(player, movePacket.toByteArray(), maxDist);
            } catch (IOException ignored) {}
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        WatutPlayerStatus status = plugin.getStatusManager().getStatus(event.getPlayer().getUniqueId());
        status.setIdleTicks(0);
        if (status.isIdle()) {
            status.setIdle(false);
        }
    }
}
