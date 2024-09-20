package org.incogn1.autoshutdown.config;

import com.moandjiezana.toml.Toml;
import net.fabricmc.loader.api.FabricLoader;
import org.incogn1.autoshutdown.AutoShutdown;

import java.io.*;
import java.nio.file.Path;

public class ConfigManager {
    private final Path configPath;

    public ConfigManager() {
        String configFileName = AutoShutdown.MOD_ID + ".toml";
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(configFileName);
    }

    public Config loadConfig() {
        if (!this.configPath.toFile().exists()) {
            genDefaultConfig();
        }

        Toml toml = new Toml();
        File configFile = configPath.toFile();
        return toml.read(configFile).to(Config.class);
    }

    private void genDefaultConfig() {
        try (InputStream inputStream = AutoShutdown.class.getClassLoader().getResourceAsStream("config.toml")) {
            if (inputStream == null) {
                AutoShutdown.LOGGER.error("Config file missing from plugin resources");
                return;
            }

            File dest = configPath.toFile();
            try (OutputStream outputStream = new FileOutputStream(dest)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            AutoShutdown.LOGGER.error("Failed to load default config", e);
        }
    }
}
