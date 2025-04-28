package it.mjicio.rareore.commands;

import it.mjicio.rareore.AziendaPlugin;
import it.mjicio.rareore.managers.CompanyManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompanyCommandExecutor implements CommandExecutor, TabCompleter {

    private final AziendaPlugin plugin;
    private final CompanyManager companyManager;

    private LuckPerms luckPerms;

    public CompanyCommandExecutor(AziendaPlugin plugin, CompanyManager companyManager) {
        this.plugin = plugin;
        this.companyManager = companyManager;
        this.luckPerms = LuckPermsProvider.get();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo i giocatori possono usare questo comando.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso corretto: /azienda [nomeazienda] [assumi/licenzia/modificaruolo/aggiungiExtra/rimuoviextra] [player] [ruolo]");
            return true;
        }

        if (sender.hasPermission(args[0] + ".direttore")) {

            String companyName = args[0];
            String action = args[1];
            String playerName = args[2];
            String role = args.length > 3 ? args[3] : "";

            Player player = (Player) sender;

      /*      User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            List<String> directorRoles = getRoles(companyName);

            boolean isDirector = false;
            for (String directorRole : directorRoles) {
                Node node = Node.builder("group." + directorRole.toLowerCase()).build();
                if (user.getNodes().contains(node)) {
                    isDirector = true;
                    break;
                }
            }

            if (!isDirector) {
                player.sendMessage(ChatColor.RED + "Non sei direttore di questa azienda!");
                return true;
            } */

            User targetUser = luckPerms.getUserManager().getUser(playerName);
            if (targetUser == null) {
                player.sendMessage(ChatColor.RED + "Il giocatore specificato non esiste.");
                return true;
            }

            switch (action.toLowerCase()) {
                case "assumi":
                    if (companyManager.isHired(playerName)) {
                        player.sendMessage(ChatColor.RED + "E' già in una azienda!");
                        return true;
                    }
                    if (role.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "Devi inserire il ruolo con cui assumere il player");
                        return true;
                    }
                    companyManager.hirePlayer(companyName, playerName, role, player);
                    break;
                case "licenzia":
                case "modificaruolo":
                case "aggiungiextra":
                case "rimuoviextra":
                   /* boolean hasRequiredRole = false;
                    for (String directorRole : directorRoles) {
                        Node node = Node.builder("group." + directorRole.toLowerCase()).build();
                        if (targetUser.getNodes().contains(node)) {
                            hasRequiredRole = true;
                            break;
                        }
                    }
                    if (!hasRequiredRole) {
                        player.sendMessage(ChatColor.RED + "Il giocatore non ha il ruolo richiesto per questa azione.");
                        return true;
                    } */
                    performAction(action, companyName, playerName, role, player);
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Azione non riconosciuta. Usa: assumi, licenzia, modificaruolo, aggiungiExtra, rimuoviextra");
                    break;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Non hai il permesso per eseguire questo comando.");
        }
        return true;
    }

    private void performAction(String action, String companyName, String playerName, String role, Player player) {
        switch (action.toLowerCase()) {
            case "licenzia":
                companyManager.firePlayer(companyName, playerName, player);
                break;
            case "modificaruolo":
                if (role.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Devi inserire il ruolo da dare al player");
                    return;
                }
                companyManager.changePlayerRole(companyName, playerName, role, player);
                break;
            case "aggiungiextra":
                if (role.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Devi inserire il ruolo da aggiungere al player");
                    return;
                }
                companyManager.addExtraToPlayer(companyName, playerName, role, player);
                break;
            case "rimuoviextra":
                if (role.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Devi inserire il ruolo da rimuovere al player");
                    return;
                }
                companyManager.removeExtraToPlayer(companyName, playerName, role, player);
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            ConfigurationSection aziendeSection = plugin.getConfigManager().getRolesConfig().getConfigurationSection("aziende");
            if (aziendeSection != null) {
                completions.addAll(aziendeSection.getKeys(false));
            }
        }

        if (args.length == 2) {
            completions.add("assumi");
            completions.add("licenzia");
            completions.add("modificaruolo");
            completions.add("aggiungiextra");
            completions.add("rimuoviextra");
        }

        if (args.length == 3) return null;

        if (args.length == 4) {
            String companyName = args[0];
            String action = args[1];
            if (action.equalsIgnoreCase("assumi") || action.equalsIgnoreCase("modificaruolo")) {
                List<String> roles = companyManager.getRoles(companyName);
                if (roles != null) {
                    completions.addAll(roles);
                }
            }
            if (action.equalsIgnoreCase("aggiungiextra") || action.equalsIgnoreCase("rimuoviextra")) {
                List<String> extras = companyManager.getExtras(companyName);
                if (extras != null) {
                    completions.addAll(extras);
                }
            }
        }

        List<String> sorted = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length-1], completions, sorted);
        Collections.sort(sorted);
        return sorted;
    }

    public List<String> getRoles(String companyName) {
        FileConfiguration config = plugin.getConfigManager().getRolesConfig();
        if (!config.contains("aziende." + companyName + ".ruoli")) {
            return Collections.emptyList();
        }
        return config.getStringList("aziende." + companyName + ".ruoli");
    }
}
