package com.corosus.watut.paper;

import com.corosus.watut.common.WatutChannels;
import com.corosus.watut.paper.update.UpdateChecker;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main JavaPlugin class for the Paper/Spigot/Folia server implementation of WATUT.
 */
public class WatutPaperPlugin extends JavaPlugin implements CommandExecutor {

    private static WatutPaperPlugin instance;
    private WatutServerStatusManager statusManager;
    private UpdateChecker updateChecker;
    private final Map<UUID, ScheduledTask> idleTasks = new ConcurrentHashMap<>();

    public static WatutPaperPlugin getInstance() {
        return instance;
    }

    public WatutServerStatusManager getStatusManager() {
        return statusManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        statusManager = new WatutServerStatusManager(this);

        // Register custom payload channels
        Bukkit.getMessenger().registerIncomingPluginChannel(this, WatutChannels.CHANNEL_CLIENT_TO_SERVER, new WatutChannelListener(this));
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, WatutChannels.CHANNEL_SERVER_TO_CLIENT);

        // Register event listener
        getServer().getPluginManager().registerEvents(new WatutEventListener(this), this);

        // Register command
        if (getCommand("watut") != null) {
            getCommand("watut").setExecutor(this);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            startPlayerIdleTask(online);
        }

        if (getConfig().getBoolean("update-checker.enabled", true)) {
            String projectId = getConfig().getString("update-checker.project-id", "what-are-they-up-to");
            String contact = getConfig().getString("update-checker.contact", "watut@localhost");
            updateChecker = new UpdateChecker(this, projectId, contact, true);
            updateChecker.check(60L, null);
        }

        getLogger().info("WATUT Paper Server Plugin v" + getDescription().getVersion() + " (Java 25) initialized successfully!");
    }

    public void startPlayerIdleTask(Player player) {
        idleTasks.compute(player.getUniqueId(), (id, existing) -> {
            if (existing != null) {
                existing.cancel();
            }
            return player.getScheduler().runAtFixedRate(this, task -> {
                if (statusManager != null) {
                    statusManager.tickIdleFallback(player);
                }
            }, null, 20L, 20L);
        });
    }

    public void stopPlayerIdleTask(UUID playerId) {
        ScheduledTask task = idleTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void onDisable() {
        for (ScheduledTask task : idleTasks.values()) {
            task.cancel();
        }
        idleTasks.clear();

        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);

        statusManager = null;
        instance = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("watut.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            reloadConfig();
            sender.sendMessage("§a[WATUT] Configuration reloaded.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("version")) {
            sender.sendMessage("§e[WATUT] §7Version: §a" + getDescription().getVersion());
            if (updateChecker != null) {
                sender.sendMessage("§7Checking Modrinth for updates...");
                updateChecker.check(checker -> {
                    if (checker.isUpdateAvailable()) {
                        sender.sendMessage("§e[WATUT] §aNew update found: " + checker.getLatestVersion() + " (" + checker.getDownloadUrl() + ")");
                    } else {
                        sender.sendMessage("§a[WATUT] You are running the latest version.");
                    }
                });
            }
            return true;
        }

        sender.sendMessage("§e[WATUT] §7Commands: §f/watut reload §7| §f/watut version");
        return true;
    }
}
