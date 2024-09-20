package org.incogn1.autoShutdown;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AutoShutdown extends JavaPlugin implements Listener {
    private BukkitTask shutdownTask;

    private long shutdownDelayTicks;
    private boolean loggingEnabled;

    @Override
    public void onEnable() {
        FileConfiguration config = this.getConfig();

        config.addDefault("initial_delay_seconds", 60L);
        config.addDefault("shutdown_delay_seconds", 300L);
        config.addDefault("enable_logging", true);
        config.options().copyDefaults(true);
        saveDefaultConfig();

        long initialDelayTicks = 20 * config.getLong("initial_delay_seconds");
        this.shutdownDelayTicks = 20 * config.getLong("shutdown_delay_seconds");
        this.loggingEnabled = config.getBoolean("enable_logging");

        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskLater(this, this::doAbandonedServerCheck, initialDelayTicks);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        // Run as task to make sure player has been removed from online players list
        Bukkit.getScheduler().runTask(this, this::doAbandonedServerCheck);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (shutdownTask == null) return;

        if (this.loggingEnabled) {
            Bukkit.getLogger().info("Shutdown process cancelled because a player joined the server within the specified delay.");
        }

        cancelShutdownTask();
    }

    private void doAbandonedServerCheck() {
        if (!getServer().getOnlinePlayers().isEmpty()) return;

        // Schedule shutdown task
        scheduleShutdownTask();
    }

    private void scheduleShutdownTask() {
        if (this.loggingEnabled) {
            Bukkit.getLogger().info("No more players online. Waiting for " + this.shutdownDelayTicks / 20 + " seconds before shutting down the server.");
        }

        shutdownTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!getServer().getOnlinePlayers().isEmpty()) return;
            Bukkit.shutdown();
        }, this.shutdownDelayTicks);
    }

    private void cancelShutdownTask() {
        shutdownTask.cancel();
        shutdownTask = null;
    }
}
