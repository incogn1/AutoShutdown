package org.incogn1.autoShutdown;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class AutoShutdown extends JavaPlugin implements Listener {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    // How often to re-check the clock while waiting for the timeframe to open, in ticks
    private static final long TIMEFRAME_RECHECK_TICKS = 20L * 600L; // 600 seconds = 10 minutes

    private BukkitTask shutdownTask;

    private long shutdownDelayTicks;
    private boolean loggingEnabled;

    private boolean timeframeEnabled;
    private LocalTime timeframeStart;
    private LocalTime timeframeEnd;

    @Override
    public void onEnable() {
        FileConfiguration config = this.getConfig();

        config.addDefault("initial_delay_seconds", 60L);
        config.addDefault("shutdown_delay_seconds", 300L);
        config.addDefault("enable_logging", true);
        config.addDefault("timeframe_enabled", false);
        config.addDefault("start_time", "00:00");
        config.addDefault("end_time", "08:00");
        config.options().copyDefaults(true);
        saveDefaultConfig();

        long initialDelayTicks = 20 * config.getLong("initial_delay_seconds");
        this.shutdownDelayTicks = 20 * config.getLong("shutdown_delay_seconds");
        this.loggingEnabled = config.getBoolean("enable_logging");

        this.timeframeEnabled = config.getBoolean("timeframe_enabled");
        this.timeframeStart = parseTime(config.getString("start_time"), LocalTime.MIDNIGHT);
        this.timeframeEnd = parseTime(config.getString("end_time"), LocalTime.of(8, 0));

        Bukkit.getLogger().info("[AutoShutdown] Initial delay set to " + initialDelayTicks / 20 + " seconds.");
        Bukkit.getLogger().info("[AutoShutdown] Shutdown delay set to " + this.shutdownDelayTicks / 20 + " seconds.");
        Bukkit.getLogger().info("[AutoShutdown] Timeframe enabled: " + this.timeframeEnabled);
        if (this.timeframeEnabled) {
            Bukkit.getLogger().info("[AutoShutdown] Timeframe: " + this.timeframeStart + " - " + this.timeframeEnd);
        }

        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskLater(this, this::doAbandonedServerCheck, initialDelayTicks);
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null) return fallback;
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeException e) {
            Bukkit.getLogger().warning("[AutoShutdown] Could not parse time '" + value
                    + "', expected format HH:mm (UTC). Falling back to " + fallback + ".");
            return fallback;
        }
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
            Bukkit.getLogger().info("[AutoShutdown] Shutdown process cancelled because a player joined the server within the specified delay.");
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
            Bukkit.getLogger().info("[AutoShutdown] No more players online. Waiting for " + this.shutdownDelayTicks / 20 + " seconds before shutting down the server.");
        }

        shutdownTask = Bukkit.getScheduler().runTaskLater(this, this::attemptShutdown, this.shutdownDelayTicks);
    }

    private void attemptShutdown() {
        if (!getServer().getOnlinePlayers().isEmpty()) return;

        if (!isWithinTimeframe()) {
            if (this.loggingEnabled) {
                Bukkit.getLogger().info("[AutoShutdown] Server is still empty but outside the allowed shutdown timeframe ("
                        + timeframeStart + " - " + timeframeEnd + " UTC). Checking again in " + TIMEFRAME_RECHECK_TICKS / 20 / 60 + " minutes.");
            }
            // Keep waiting for the timeframe to open, re-checking periodically
            shutdownTask = Bukkit.getScheduler().runTaskLater(this, this::attemptShutdown, TIMEFRAME_RECHECK_TICKS); 
            return;
        }

        Bukkit.shutdown();
    }

    /**
     * @return true if shutdown is allowed right now: either the timeframe restriction is
     * disabled, or the current UTC time falls within the configured start/end window.
     * Handles windows that wrap past midnight (e.g. 22:00 - 06:00).
     */
    private boolean isWithinTimeframe() {
        if (!this.timeframeEnabled) return true;

        LocalTime now = LocalTime.now(ZoneOffset.UTC);

        if (timeframeStart.equals(timeframeEnd)) {
            // Zero-width or full-day window: treat as "always allowed"
            return true;
        }

        if (timeframeStart.isBefore(timeframeEnd)) {
            // Normal window, e.g. 00:00 - 08:00
            return !now.isBefore(timeframeStart) && now.isBefore(timeframeEnd);
        } else {
            // Wrapping window, e.g. 22:00 - 06:00
            return !now.isBefore(timeframeStart) || now.isBefore(timeframeEnd);
        }
    }

    private void cancelShutdownTask() {
        shutdownTask.cancel();
        shutdownTask = null;
    }
}