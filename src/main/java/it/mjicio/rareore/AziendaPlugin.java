package it.mjicio.rareore;

import it.mjicio.rareore.commands.CompanyCommandExecutor;
import it.mjicio.rareore.managers.CompanyManager;
import it.mjicio.rareore.managers.ConfigManager;
import it.mjicio.rareore.utils.DiscordManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AziendaPlugin extends JavaPlugin {

    private CompanyManager companyManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        DiscordManager discordManager = new DiscordManager(this);

        // Inizializzare i manager
        this.configManager = new ConfigManager(this);
        this.companyManager = new CompanyManager(this, discordManager);

        // Registrare i comandi
        this.getCommand("azienda").setExecutor(new CompanyCommandExecutor(this, companyManager));

        // Caricare la configurazione
        configManager.loadRolesConfig();

        discordManager.init();

    }

    @Override
    public void onDisable() {
        // Salvare la configurazione
        configManager.saveRolesConfig();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
