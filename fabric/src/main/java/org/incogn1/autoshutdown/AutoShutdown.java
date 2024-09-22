package org.incogn1.autoshutdown;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.incogn1.autoshutdown.config.Config;
import org.incogn1.autoshutdown.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoShutdown implements ModInitializer {
	public static final String MOD_ID = "autoshutdown";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private boolean loggingEnabled;

	private long initialDelayMillis;
	private long lastOnlineMillis;
    private long pollingDelayMillis;
	private long shutdownDelayMillis;
	private boolean inDelayedShutdownProcess;

	@Override
	public void onInitialize() {
		Config config = new ConfigManager().loadConfig();

		this.loggingEnabled = config.getLogging().getEnabled();

        this.initialDelayMillis = config.getDelays().getInitial() * 1000L;
		this.pollingDelayMillis = config.getDelays().getPolling() * 1000L;
		this.shutdownDelayMillis = config.getDelays().getShutdown() * 1000L;

		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
		ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
	}

	private void onServerStarted(MinecraftServer server) {
		this.lastOnlineMillis = System.currentTimeMillis() + initialDelayMillis;
	}

	private void onServerTick(MinecraftServer server) {
		long currentTimeMillis = System.currentTimeMillis();

		// Throttle polling rate
		if (currentTimeMillis - lastOnlineMillis < pollingDelayMillis) return;

		// Still players online check
		int playerCount = server.getCurrentPlayerCount();
		if (playerCount > 0) {
			if (inDelayedShutdownProcess) {
				if (loggingEnabled) {
					LOGGER.info("Shutdown process cancelled because a player joined the server within the specified delay.");
				}
				inDelayedShutdownProcess = false;
			}

			lastOnlineMillis = currentTimeMillis;

			return;
		}

		if (!inDelayedShutdownProcess) {
			if (loggingEnabled) {
				LOGGER.info("No more players online. Waiting for {} seconds before shutting down the server.", shutdownDelayMillis / 1000);
			}
			inDelayedShutdownProcess = true;
		}

		// Guard - Shutdown delay not expired yet
		if (currentTimeMillis - lastOnlineMillis < shutdownDelayMillis) return;

		doServerShutdown(server);
	}

	private void doServerShutdown(MinecraftServer server) {
		server.stop(false);
	}
}