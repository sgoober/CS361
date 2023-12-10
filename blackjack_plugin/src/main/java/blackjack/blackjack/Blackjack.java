package blackjack.blackjack;

import blackjack.blackjack.commands.blackjackstart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public final class Blackjack extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getLogger().info("*****************************");
        Bukkit.getLogger().info("There is an update available for Blackjack!");
        Bukkit.getLogger().info("Current version: 0.1 [3KB]");
        Bukkit.getLogger().info("Current version: 0.1.0.1 [4KB]");
        Bukkit.getLogger().info("Reason: Potential security exploit bug fixed");
        Bukkit.getLogger().info("To download latest version, Visit: spigotmc.org/resources/blackjack");
        Bukkit.getLogger().info("*****************************");
        getCommand("blackjackstart").setExecutor(new blackjackstart(this));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.broadcastMessage("Use '/blackjackstart' to start game.\nUse /help blackjackstart for help and additional information.");
        }, 1200L);


    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getLogger().info("blackjack closing");
    }
}
