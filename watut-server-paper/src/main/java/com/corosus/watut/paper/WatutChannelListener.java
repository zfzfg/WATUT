package com.corosus.watut.paper;

import com.corosus.watut.common.WatutChannels;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Listens for incoming plugin channel messages from the WATUT client mod (watut:nbt_server).
 */
public class WatutChannelListener implements PluginMessageListener {

    private final WatutPaperPlugin plugin;

    public WatutChannelListener(WatutPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!WatutChannels.CHANNEL_CLIENT_TO_SERVER.equals(channel)) {
            return;
        }

        plugin.getStatusManager().handleClientPacket(player, message);
    }
}
