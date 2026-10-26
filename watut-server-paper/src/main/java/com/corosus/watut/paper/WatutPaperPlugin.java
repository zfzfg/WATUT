package com.corosus.watut.paper;

import com.corosus.watut.common.WatutChannels;
import com.corosus.watut.paper.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Main JavaPlugin class for the Paper/Spigot server implementation of WATUT.
 */
public class WatutPaperPlugin extends JavaPlugin implements CommandExecutor {

    private static WatutPaperPlugin instance;
    private WatutServerStatusManager statusManager;
    private UpdateChecker updateChecker;
    private BukkitTask idleTask;

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

        // Start repeating task for idle status check (every 20 ticks / 1 second)
        idleTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (statusManager != null) {
                statusManager.tickIdleCheck();
            }
        }, 20L, 20L);

        // Start Modrinth update check
        if (getConfig().getBoolean("update-checker.enabled", true)) {
            String projectId = getConfig().getString("update-checker.project-id", "what-are-they-up-to");
            updateChecker = new UpdateChecker(this, projectId, "admin@server.local", true);
            updateChecker.check(60L, null); // Check asynchronously after 3 seconds
        }

        getLogger().info("WATUT Paper Server Plugin v" + getDescription().getVersion() + " (Java 25) initialized successfully!");
    }

    @Override
    public void onDisable() {
        if (idleTask != null) {
            idleTask.cancel();
            idleTask = null;
        }

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
