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
    FileConfiguration config = this.getConfig();
    BukkitTask shutdownTask;

    @Override
    public void onEnable() {
        config.addDefault("shutdown_delay_seconds", 300L);
        config.addDefault("disable_logging", false);
        config.options().copyDefaults(true);
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {

        // Run as task to make sure player has been removed from online players list
        shutdownTask = Bukkit.getScheduler().runTask(this, () -> {
            if (!getServer().getOnlinePlayers().isEmpty()) return;
            long delaySeconds = config.getLong("shutdown_delay_seconds");
            long delayTicks = 20L * delaySeconds; // 20 ticks = 1 sec

            if (!config.getBoolean("disable_logging")) {
                Bukkit.getLogger().info("No more players online. Waiting for " + delaySeconds + " seconds before shutting down the server.");
            }

            // Schedule shutdown task
            shutdownTask = Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!getServer().getOnlinePlayers().isEmpty()) return;
                Bukkit.shutdown();
            }, delayTicks);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (shutdownTask != null) {
            if (!config.getBoolean("disable_logging")) {
                Bukkit.getLogger().info("Shutdown process cancelled because a player joined the server within the specified delay.");
            }

            shutdownTask.cancel();
        }
    }
}
