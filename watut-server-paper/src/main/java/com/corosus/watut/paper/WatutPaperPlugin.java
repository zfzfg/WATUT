package com.corosus.watut.paper;

import com.corosus.watut.common.WatutChannels;
import com.corosus.watut.paper.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main JavaPlugin class for the Paper/Spigot/Folia server implementation of WATUT.
 */
public class WatutPaperPlugin extends JavaPlugin implements CommandExecutor {

    private static WatutPaperPlugin instance;
    private WatutServerStatusManager statusManager;
    private UpdateChecker updateChecker;
    private AutoCloseable idleTaskCanceller;

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

        // Schedule idle status check (every 20 ticks / 1 second) with Folia / Paper compatibility
        startIdleTask();

        // Start Modrinth update check
        if (getConfig().getBoolean("update-checker.enabled", true)) {
            String projectId = getConfig().getString("update-checker.project-id", "what-are-they-up-to");
            updateChecker = new UpdateChecker(this, projectId, "admin@server.local", true);
            updateChecker.check(60L, null); // Check asynchronously after 3 seconds
        }

        getLogger().info("WATUT Paper Server Plugin v" + getDescription().getVersion() + " (Java 25) initialized successfully!");
    }

    private void startIdleTask() {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                java.lang.reflect.Method runMethod = scheduler.getClass().getMethod("runAtFixedRate",
                        org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class, long.class);

                final AtomicReference<Object> scheduledTaskRef = new AtomicReference<>();
                java.util.function.Consumer<Object> taskConsumer = task -> {
                    if (statusManager != null) {
                        statusManager.tickIdleCheck();
                    }
                };

                Object task = runMethod.invoke(scheduler, this, taskConsumer, 20L, 20L);
                scheduledTaskRef.set(task);

                idleTaskCanceller = () -> {
                    Object t = scheduledTaskRef.get();
                    if (t != null) {
                        try {
                            t.getClass().getMethod("cancel").invoke(t);
                        } catch (Exception ignored) {}
                    }
                };
                return;
            } catch (Exception e) {
                getLogger().warning("Failed to initialize Folia global scheduler, falling back to Bukkit scheduler: " + e.getMessage());
            }
        }

        // Standard Paper / Spigot scheduler fallback
        var bukkitTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (statusManager != null) {
                statusManager.tickIdleCheck();
            }
        }, 20L, 20L);
        idleTaskCanceller = bukkitTask::cancel;
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onDisable() {
        if (idleTaskCanceller != null) {
            try {
                idleTaskCanceller.close();
            } catch (Exception ignored) {}
            idleTaskCanceller = null;
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
