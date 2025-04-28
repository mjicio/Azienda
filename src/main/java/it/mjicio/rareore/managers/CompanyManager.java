package it.mjicio.rareore.managers;

import it.mjicio.rareore.AziendaPlugin;
import it.mjicio.rareore.utils.DiscordManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CompanyManager {

    private final AziendaPlugin plugin;
    private final LuckPerms luckPerms;

    private final DiscordManager discordManager;

    public CompanyManager(AziendaPlugin plugin, DiscordManager discordManager) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
        this.discordManager = discordManager;
    }

    private boolean checksFailed(String companyName, String playerName, String role, Player executor, boolean isHireProcess, boolean isFireProcess, boolean isExtraProcess) {
        FileConfiguration config = plugin.getConfigManager().getRolesConfig();
        if (!config.contains("aziende." + companyName)) {
            executor.sendMessage(ChatColor.RED + "L'azienda " + companyName + " non esiste.");
            return true;
        }

        if (!isFireProcess) {
            if (isExtraProcess) {
                List<String> roleList = new ArrayList<>(config.getStringList("aziende." + companyName + ".extra"));
                boolean containsRole = roleList.stream().anyMatch(role::equalsIgnoreCase);
                if (!containsRole) {
                    executor.sendMessage(ChatColor.RED + "Il ruolo extra " + role + " non esiste nell'azienda " + companyName);
                    return true;
                }
            } else {
                List<String> roleList = new ArrayList<>(config.getStringList("aziende." + companyName + ".ruoli"));
                boolean containsRole = roleList.stream().anyMatch(role::equalsIgnoreCase);
                if (!containsRole) {
                    executor.sendMessage(ChatColor.RED + "Il ruolo " + role + " non esiste nell'azienda " + companyName);
                    return true;
                }
            }

        }

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            executor.sendMessage(ChatColor.RED + "Il giocatore " + playerName + " non è online.");
            return true;
        }

        User user = luckPerms.getUserManager().getUser(playerName);
        if (user == null) return true;

        if (!isHireProcess) if (!isHiredTo(playerName, companyName)) {
            executor.sendMessage(ChatColor.RED + "Il giocatore " + playerName + " non è assunto all'azienda " + companyName);
            return true;
        }

        return false;
    }

    public void hirePlayer(String companyName, String playerName, String role, Player executor) {
        if (checksFailed(companyName, playerName, role, executor, true, false, false)) return;
        Player targetPlayer = Bukkit.getPlayer(playerName);
        User user = luckPerms.getUserManager().getUser(playerName);
        assert targetPlayer != null;
        assert user != null;

        // Ottiene tutti i gruppi attuali del player
        Set<String> playerGroups = user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());

        // Aggiunge il ruolo al player e rimuove i ruoli dell'azienda
        FileConfiguration config = plugin.getConfigManager().getRolesConfig();
        List<String> roleList = new ArrayList<>(config.getStringList("aziende." + companyName + ".ruoli"));
        List<String> extraRoleList = new ArrayList<>(config.getStringList("aziende." + companyName + ".extra"));
        luckPerms.getUserManager().modifyUser(targetPlayer.getUniqueId(), modifiedUser -> {
            modifiedUser.data().add(InheritanceNode.builder(role.toLowerCase()).build());
            playerGroups.forEach(group -> {
                if (roleList.contains(group) || extraRoleList.contains(group))
                    modifiedUser.data().remove(InheritanceNode.builder(group).build());
            });
        });

        executor.sendMessage(ChatColor.GREEN + "Hai assunto " + playerName + " come " + role + " in " + companyName);
        targetPlayer.sendMessage(ChatColor.GREEN + "Sei stato assunto come " + role + " in " + companyName);
        discordManager.sendMessage("1266161000520880188", "**AZIENDA** *" + playerName + "* è stato assunto in **" + companyName + "** da " + executor.getName());
    }

    public void firePlayer(String companyName, String playerName, Player executor) {
        if (checksFailed(companyName, playerName, "licenziamento", executor, false, true, false)) return;
        Player targetPlayer = Bukkit.getPlayer(playerName);
        User user = luckPerms.getUserManager().getUser(playerName);
        assert targetPlayer != null;
        assert user != null;

        // Ottiene tutti i gruppi attuali del player
        Set<String> playerGroups = user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());

        // Rimuove i gruppi dell'azienda
        FileConfiguration config = plugin.getConfigManager().getRolesConfig();
        List<String> roleList = new ArrayList<>(config.getStringList("aziende." + companyName + ".ruoli"));
        List<String> extraRoleList = new ArrayList<>(config.getStringList("aziende." + companyName + ".extra"));
        luckPerms.getUserManager().modifyUser(targetPlayer.getUniqueId(), modifiedUser -> playerGroups.forEach(group -> {
            if (roleList.contains(group) || extraRoleList.contains(group))
                modifiedUser.data().remove(InheritanceNode.builder(group).build());
        }));

        executor.sendMessage(ChatColor.GREEN + "Hai licenziato " + playerName + " da " + companyName);
        targetPlayer.sendMessage(ChatColor.RED + "Sei stato licenziato da " + companyName);
        discordManager.sendMessage("1266161000520880188", "**AZIENDA** *" + playerName + "* è stato licenziato da **" + companyName + "** da " + executor.getName());
    }



    public void changePlayerRole(String companyName, String playerName, String role, Player executor) {
        if (checksFailed(companyName, playerName, role, executor, false, false, false)) return;
        Player targetPlayer = Bukkit.getPlayer(playerName);
        User user = luckPerms.getUserManager().getUser(playerName);
        assert targetPlayer != null;
        assert user != null;

        // Ottiene tutti i gruppi attuali del player
        Set<String> playerGroups = user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());

        // Rimuove i gruppi dell'azienda (non quelli extra) e aggiunge quello nuovo
        FileConfiguration config = plugin.getConfigManager().getRolesConfig();
        List<String> roleList = new ArrayList<>(config.getStringList("aziende." + companyName + ".ruoli"));
        luckPerms.getUserManager().modifyUser(targetPlayer.getUniqueId(), modifiedUser -> {
            modifiedUser.data().add(InheritanceNode.builder(role.toLowerCase()).build());
            playerGroups.forEach(group -> {
                if (roleList.contains(group))
                    modifiedUser.data().remove(InheritanceNode.builder(group).build());
            });
        });

        executor.sendMessage(ChatColor.GREEN + "Hai cambiato il ruolo di " + playerName + " a " + role + " in " + companyName);
        targetPlayer.sendMessage(ChatColor.GREEN + "Il tuo ruolo è cambiato a " + role + " in " + companyName);
        discordManager.sendMessage("1266161000520880188", "**AZIENDA** Il ruolo di *" + playerName + "* è stato cambiato in **" + role + "** nell'azienda **" + companyName + "** da " + executor.getName());
    }

    public void addExtraToPlayer(String companyName, String playerName, String extra, Player executor) {
        if (checksFailed(companyName, playerName, extra, executor, false, false, true)) return;
        Player targetPlayer = Bukkit.getPlayer(playerName);
        User user = luckPerms.getUserManager().getUser(playerName);
        assert targetPlayer != null;
        assert user != null;

        // Aggiunge un ruolo al player
        luckPerms.getUserManager().modifyUser(targetPlayer.getUniqueId(), modifiedUser -> modifiedUser.data().add(InheritanceNode.builder(extra.toLowerCase()).build()));

        executor.sendMessage(ChatColor.GREEN + "Hai aggiunto " + extra + " a " + playerName + " in " + companyName);
        targetPlayer.sendMessage(ChatColor.GREEN + "Hai ricevuto un extra: " + extra + " in " + companyName);
        discordManager.sendMessage("1266161000520880188", "**AZIENDA** A *" + playerName + "* è stato aggiunto il ruolo extra **" + extra + "** in **" + companyName + "** da " + executor.getName());
    }


    public void removeExtraToPlayer(String companyName, String playerName, String extra, Player executor) {
        if (checksFailed(companyName, playerName, extra, executor, false, false, true)) return;
        Player targetPlayer = Bukkit.getPlayer(playerName);
        User user = luckPerms.getUserManager().getUser(playerName);
        assert targetPlayer != null;
        assert user != null;

        // Ottiene tutti i gruppi attuali del player
        Set<String> playerGroups = user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());

        // Rimuove i gruppi dell'azienda
        luckPerms.getUserManager().modifyUser(targetPlayer.getUniqueId(), modifiedUser -> {
            if (playerGroups.contains(extra.toLowerCase()))
                modifiedUser.data().remove(InheritanceNode.builder(extra.toLowerCase()).build());

        });

        executor.sendMessage(ChatColor.GREEN + "Hai rimosso " + extra + " a " + playerName + " in " + companyName);
        targetPlayer.sendMessage(ChatColor.GREEN + "Ti hanno rimosso un extra: " + extra + " in " + companyName);
        discordManager.sendMessage("1266161000520880188", "**AZIENDA** A *" + playerName + "* è stato rimosso il ruolo extra **" + extra + "** in **" + companyName + "** da " + executor.getName());
    }

    public List<String> getRoles(String companyName) {
        FileConfiguration config = plugin.getConfigManager().getRolesConfig();
        if (!config.contains("aziende." + companyName + ".ruoli")) {
            return Collections.emptyList();
        }
        return config.getStringList("aziende." + companyName + ".ruoli");
    }

    public List<String> getExtras(String companyName) {
        FileConfiguration config = plugin.getConfigManager().getRolesConfig();
        if (!config.contains("aziende." + companyName + ".extra")) {
            return Collections.emptyList();
        }
        return config.getStringList("aziende." + companyName + ".extra");
    }

    public Set<String> getCompanies() {
        FileConfiguration config = plugin.getConfigManager().getRolesConfig();
        ConfigurationSection aziende = config.getConfigurationSection("aziende");
        if (aziende == null) {
            return Collections.emptySet();
        }
        return aziende.getKeys(false);
    }

    public boolean isHired(String playerString) {
        Player targetPlayer = Bukkit.getPlayer(playerString);
        if (targetPlayer == null) return false;

        User user = luckPerms.getUserManager().getUser(targetPlayer.getName());
        if (user == null) return false;

        Set<String> playerGroups = user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());

        AtomicBoolean isHired = new AtomicBoolean(false);
        getCompanies().forEach(company -> getRoles(company).forEach(role -> {
            if (playerGroups.contains(role.toLowerCase())) {
                isHired.set(true);
            }
        }));
        return isHired.get();
    }

    public boolean isHiredTo(String playerString, String company) {
        Player targetPlayer = Bukkit.getPlayer(playerString);
        if (targetPlayer == null) return false;

        User user = luckPerms.getUserManager().getUser(targetPlayer.getName());
        if (user == null) return false;

        Set<String> playerGroups = user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());

        AtomicBoolean isHired = new AtomicBoolean(false);
        getRoles(company).forEach(role -> {
            if (playerGroups.contains(role.toLowerCase())) {
                isHired.set(true);
            }
        });
        return isHired.get();
    }
}
