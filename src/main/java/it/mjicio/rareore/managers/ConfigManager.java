package it.mjicio.rareore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final JavaPlugin plugin;
    private File rolesFile;
    private FileConfiguration rolesConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadRolesConfig() {
        rolesFile = new File(plugin.getDataFolder(), "ruoli.yml");
        if (!rolesFile.exists()) {
            boolean success = rolesFile.getParentFile().mkdirs();
            plugin.saveResource("ruoli.yml", false);
        }

        rolesConfig = YamlConfiguration.loadConfiguration(rolesFile);
    }

    public void saveRolesConfig() {
        try {
            rolesConfig.save(rolesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getRolesConfig() {
        return rolesConfig;
    }
}
