package blackjack.blackjack.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.function.Supplier;
import java.util.ArrayList;


public class blackjackstart implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }
        Player player = (Player) sender;

        Location playerPos = player.getLocation();

        Inventory blackjackgame = Bukkit.createInventory(null, 27, "Blackjack [Choose Hit/Stand]");
        blackjackgame.setItem(3, getItem(new ItemStack(Material.RED_BANNER), "&8Hidden Card"));
        blackjackgame.setItem(4, getItem(new ItemStack(Material.RED_BANNER), "&8Hidden Card"));
        blackjackgame.setItem(15, getItem(new ItemStack(Material.LIME_WOOL), "&aHit", "&8Draw an additional card"));
        blackjackgame.setItem(16, getItem(new ItemStack(Material.BARRIER), "&4Stand", "&8Finish your turn"));
        blackjackgame.setItem(21, getItem(new ItemStack(Material.RED_BANNER), "&8Hidden Card"));
        blackjackgame.setItem(22, getItem(new ItemStack(Material.RED_BANNER), "&8Hidden Card"));
        player.openInventory(blackjackgame);


        return true;
    }
    private ItemStack getItem(ItemStack item, String name, String ... lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lores = new ArrayList<>();
        for(String s : lore) {
            lores.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        meta.setLore(lores);
        item.setItemMeta(meta);
        return item;


    }


}
