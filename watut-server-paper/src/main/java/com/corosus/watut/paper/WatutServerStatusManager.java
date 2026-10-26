package com.corosus.watut.paper;

import com.corosus.watut.common.PlayerChatState;
import com.corosus.watut.common.PlayerGuiState;
import com.corosus.watut.common.WatutChannels;
import com.corosus.watut.common.WatutPlayerStatus;
import com.corosus.watut.common.nbt.NbtCompound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages server-side player status tracking, idle timers, and plugin channel relay.
 */
public class WatutServerStatusManager {

    private final WatutPaperPlugin plugin;
    private final Map<UUID, WatutPlayerStatus> playerStatuses = new ConcurrentHashMap<>();
    private final Set<UUID> suppressScreenSeries = ConcurrentHashMap.newKeySet();

    public WatutServerStatusManager(WatutPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public WatutPlayerStatus getStatus(UUID uuid) {
        return playerStatuses.computeIfAbsent(uuid, WatutPlayerStatus::new);
    }

    public void removePlayer(UUID uuid) {
        playerStatuses.remove(uuid);
        suppressScreenSeries.remove(uuid);
    }

    /**
     * Handles incoming packet from client over watut:nbt_server channel,
     * unpacks payload, injects player UUID, applies adaptive bandwidth controls,
     * and forwards to nearby or all players.
     */
    public void handleClientPacket(Player player, byte[] message) {
        if (message == null || message.length == 0) {
            return;
        }

        try {
            NbtCompound compound = NbtCompound.fromByteArray(message);
            WatutPlayerStatus status = getStatus(player.getUniqueId());

            // 1. Inject the sender's UUID
            compound.putUUID(WatutChannels.NBT_PLAYER_UUID, player.getUniqueId());

            // 2. Track GUI Status
            if (compound.contains(WatutChannels.NBT_PLAYER_GUI_STATUS)) {
                int guiOrdinal = compound.getInt(WatutChannels.NBT_PLAYER_GUI_STATUS);
                status.setGuiState(PlayerGuiState.fromOrdinal(guiOrdinal));
            }

            // 3. Track Chat Status
            if (compound.contains(WatutChannels.NBT_PLAYER_CHAT_STATUS)) {
                int chatOrdinal = compound.getInt(WatutChannels.NBT_PLAYER_CHAT_STATUS);
                status.setChatState(PlayerChatState.fromOrdinal(chatOrdinal));
            }

            // 4. Track Idle Status
            if (compound.contains(WatutChannels.NBT_PLAYER_IDLE_TICKS)) {
                int idleTicks = compound.getInt(WatutChannels.NBT_PLAYER_IDLE_TICKS);
                status.setClientIdleAuthoritative(true);
                applyIdle(player, status, idleTicks);
                compound.putInt(WatutChannels.NBT_PLAYER_IDLE_TICKS, status.getIdleTicks());
                int ticksToIdle = plugin.getConfig().getInt("ticksToMarkPlayerIdle", 1200);
                compound.putInt(WatutChannels.NBT_PLAYER_TICKS_TO_GO_IDLE, ticksToIdle);
            }

            // 5. Track Mouse position
            if (compound.contains(WatutChannels.NBT_PLAYER_MOUSE_X)) {
                status.setMouseX(compound.getFloat(WatutChannels.NBT_PLAYER_MOUSE_X));
                status.setMouseY(compound.getFloat(WatutChannels.NBT_PLAYER_MOUSE_Y));
                status.setMousePressed(compound.getBoolean(WatutChannels.NBT_PLAYER_MOUSE_PRESSED));
            }

            // 6. Adaptive Bandwidth Management (Configurable)
            if (compound.contains(WatutChannels.NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA)) {
                stripScreenSeriesIfNeeded(player, compound);
            }

            // 7. Serialize modified payload
            byte[] outgoingData = compound.toByteArray();

            // 8. Relay to appropriate recipients
            double maxDist = plugin.getConfig().getDouble("distanceRequiredToShowGUIInfo", 16.0);
            if (compound.contains(WatutChannels.NBT_PLAYER_GUI_STATUS)
                    || compound.contains(WatutChannels.NBT_PLAYER_IDLE_TICKS)
                    || compound.contains(WatutChannels.NBT_PLAYER_CHAT_STATUS)) {
                sendToAll(outgoingData);
            } else {
                sendToNearby(player, outgoingData, maxDist);
            }

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to decode/relay WATUT packet from " + player.getName(), e);
        }
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

    public int countNearbyPlayers(Player origin, double distance) {
        Location originLoc = origin.getLocation();
        double distSq = distance * distance;
        int count = 0;
        for (Player target : origin.getWorld().getPlayers()) {
            if (target.equals(origin)) continue;
            if (target.getLocation().distanceSquared(originLoc) <= distSq) {
                count++;
            }
        }
        return count;
    }

    /**
     * Synchronizes active player states and server config to a newly joined player.
     */
    public void syncAllToPlayer(Player target) {
        // Send server configuration
        try {
            NbtCompound configNbt = new NbtCompound();
            configNbt.putBoolean(WatutChannels.NBT_SERVER_CONFIG, true);
            configNbt.putInt(WatutChannels.NBT_PLAYER_TICKS_TO_GO_IDLE, plugin.getConfig().getInt("ticksToMarkPlayerIdle", 1200));
            configNbt.putBoolean(WatutChannels.NBT_PLAYER_GUI_DONT_SEND_DETAILED_GUI_INFO, plugin.getConfig().getBoolean("dontSendDetailedGUIInfo", false));
            configNbt.putBoolean(WatutChannels.NBT_PLAYER_GUI_DONT_SEND_ITEM_INFO, plugin.getConfig().getBoolean("dontSendDetailedItemInfo", false));
            sendToPlayer(target, configNbt.toByteArray());

            // Sync currently known player statuses
            for (Map.Entry<UUID, WatutPlayerStatus> entry : playerStatuses.entrySet()) {
                if (entry.getKey().equals(target.getUniqueId())) continue;

                WatutPlayerStatus status = entry.getValue();
                NbtCompound statusNbt = new NbtCompound();
                statusNbt.putUUID(WatutChannels.NBT_PLAYER_UUID, entry.getKey());
                statusNbt.putInt(WatutChannels.NBT_PLAYER_GUI_STATUS, status.getGuiState().ordinal());
                statusNbt.putInt(WatutChannels.NBT_PLAYER_CHAT_STATUS, status.getChatState().ordinal());
                statusNbt.putInt(WatutChannels.NBT_PLAYER_IDLE_TICKS, status.getIdleTicks());
                sendToPlayer(target, statusNbt.toByteArray());
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send initial sync to " + target.getName(), e);
        }
    }

    /**
     * Fallback idle clock for players whose client never sends idle ticks.
     * Must run on the player's region thread.
     */
    public void tickIdleFallback(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        WatutPlayerStatus status = getStatus(player.getUniqueId());
        if (status.isClientIdleAuthoritative()) {
            return;
        }
        applyIdle(player, status, status.getIdleTicks() + 20);
    }

    public void noteActivity(Player player) {
        WatutPlayerStatus status = getStatus(player.getUniqueId());
        if (status.isClientIdleAuthoritative()) {
            return;
        }
        applyIdle(player, status, 0);
    }

    private void applyIdle(Player player, WatutPlayerStatus status, int ticks) {
        int ticksToIdle = plugin.getConfig().getInt("ticksToMarkPlayerIdle", 1200);
        boolean announce = plugin.getConfig().getBoolean("announceIdleStatesInChat", false);
        boolean nowIdle = ticks > ticksToIdle;
        if (nowIdle != status.isIdle() && announce) {
            String message = nowIdle
                    ? "§7" + player.getName() + " has gone idle."
                    : "§7" + player.getName() + " is no longer idle.";
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> Bukkit.broadcastMessage(message));
        }
        status.setIdleTicks(ticks);
        status.setIdle(nowIdle);
    }

    private void stripScreenSeriesIfNeeded(Player player, NbtCompound compound) {
        boolean adaptiveEnabled = plugin.getConfig().getBoolean("adaptive-bandwidth.enabled", false);
        int packetCount = compound.getInt(WatutChannels.NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA_PACKET_COUNT);
        int packetIndex = compound.getInt(WatutChannels.NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA_PACKET_INDEX);
        if (packetCount <= 0) {
            packetCount = 1;
        }
        if (packetIndex <= 1) {
            boolean overBudget = false;
            if (adaptiveEnabled) {
                int pingThreshold = plugin.getConfig().getInt("adaptive-bandwidth.ping-threshold-ms", 180);
                int nearbyThreshold = plugin.getConfig().getInt("adaptive-bandwidth.nearby-player-threshold", 8);
                double maxDist = plugin.getConfig().getDouble("distanceRequiredToShowGUIInfo", 16.0);
                overBudget = player.getPing() >= pingThreshold || countNearbyPlayers(player, maxDist) >= nearbyThreshold;
            }
            if (overBudget) {
                suppressScreenSeries.add(player.getUniqueId());
            } else {
                suppressScreenSeries.remove(player.getUniqueId());
            }
        }
        if (!suppressScreenSeries.contains(player.getUniqueId())) {
            return;
        }
        compound.remove(WatutChannels.NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA);
        compound.remove(WatutChannels.NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA_PACKET_COUNT);
        compound.remove(WatutChannels.NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA_PACKET_INDEX);
        compound.remove(WatutChannels.NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA_SIZE);
        compound.remove(WatutChannels.NBT_PLAYER_SCREEN_WIDTH);
        compound.remove(WatutChannels.NBT_PLAYER_SCREEN_HEIGHT);
        if (packetIndex >= packetCount) {
            suppressScreenSeries.remove(player.getUniqueId());
        }
    }

    public Map<UUID, WatutPlayerStatus> getPlayerStatuses() {
        return playerStatuses;
    }
}
