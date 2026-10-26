package com.corosus.watut.paper;

import com.corosus.watut.common.WatutChannels;
import com.corosus.watut.common.WatutPlayerStatus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages server-side player status tracking, idle timers, and plugin channel relay.
 */
public class WatutServerStatusManager {

    private final WatutPaperPlugin plugin;
    private final Map<UUID, WatutPlayerStatus> playerStatuses = new ConcurrentHashMap<>();

    public WatutServerStatusManager(WatutPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public WatutPlayerStatus getStatus(UUID uuid) {
        return playerStatuses.computeIfAbsent(uuid, WatutPlayerStatus::new);
    }

    public void removePlayer(UUID uuid) {
        playerStatuses.remove(uuid);
    }

    public void handleClientPacket(Player player, byte[] message) {
        // Relay packet to relevant players via plugin channel
        // In full implementation, bytes are unpacked, playerUuid injected, and forwarded.
        // For local GUI/preview info, forward to nearby players:
        double maxDist = plugin.getConfig().getDouble("distanceRequiredToShowGUIInfo", 16.0);
        sendToNearby(player, message, maxDist);
    }

    public void sendToAll(byte[] data) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendPluginMessage(plugin, WatutChannels.CHANNEL_SERVER_TO_CLIENT, data);
        }
    }

    public void sendToPlayer(Player player, byte[] data) {
        if (player != null && player.isOnline()) {
            player.sendPluginMessage(plugin, WatutChannels.CHANNEL_SERVER_TO_CLIENT, data);
        }
    }

    public void sendToNearby(Player origin, byte[] data, double distance) {
        Location originLoc = origin.getLocation();
        double distSq = distance * distance;

        for (Player target : origin.getWorld().getPlayers()) {
            if (target.equals(origin)) continue;
            if (target.getLocation().distanceSquared(originLoc) <= distSq) {
                target.sendPluginMessage(plugin, WatutChannels.CHANNEL_SERVER_TO_CLIENT, data);
            }
        }
    }

    public void tickIdleCheck() {
        int ticksToIdle = plugin.getConfig().getInt("ticksToMarkPlayerIdle", 1200);
        boolean announce = plugin.getConfig().getBoolean("announceIdleStatesInChat", false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            WatutPlayerStatus status = getStatus(player.getUniqueId());
            status.setIdleTicks(status.getIdleTicks() + 20); // called every 20 ticks (1 sec)

            if (status.getIdleTicks() >= ticksToIdle) {
                if (!status.isIdle()) {
                    status.setIdle(true);
                    if (announce) {
                        Bukkit.broadcastMessage(player.getName() + " is now idle.");
                    }
                }
            } else {
                if (status.isIdle()) {
                    status.setIdle(false);
                    if (announce) {
                        Bukkit.broadcastMessage(player.getName() + " is no longer idle.");
                    }
                }
            }
        }
    }

    public Map<UUID, WatutPlayerStatus> getPlayerStatuses() {
        return playerStatuses;
    }
}
