package blackjack.blackjack.commands;

import blackjack.blackjack.Blackjack;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.zeromq.SocketType;


public class blackjackstart implements Listener, CommandExecutor {
    private String inventoryName = "Blackjack [Choose Hit/Stand]";
    private final BlackjackSocket blackjackSocket;

    private Inventory Blackjackgame;

    private Player player;

    private String Reply;


    public blackjackstart(Blackjack plugin) {
        this.blackjackSocket = new BlackjackSocket();
        this.blackjackSocket.connect();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!event.getView().getTitle().equals(inventoryName)){
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        if(slot == 15){
            blackjackGamehit(Blackjackgame);
        }
        if(slot == 16){
            blackjackGamestand(Blackjackgame);
        }
        event.setCancelled(true);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        String startMessage = "start";
        String nextMessage = "next";

        blackjackSocket.getSocket().send(startMessage.getBytes(ZMQ.CHARSET), 0);
        Bukkit.getLogger().info("sent: " + startMessage);
        byte[] reply = blackjackSocket.getSocket().recv(0);
        Bukkit.getLogger().info("1recieved: " + new String(reply, ZMQ.CHARSET));
        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        Bukkit.getLogger().info("sent: " + nextMessage);
        reply = blackjackSocket.getSocket().recv(0);
        Bukkit.getLogger().info("2recieved: " + new String(reply, ZMQ.CHARSET));

        Player player = (Player) sender;

        //INITIAL DEALING
        Inventory blackjackgame = Bukkit.createInventory(null, 27, inventoryName);

        //Dealers
        blackjackgame.setItem(3, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));
        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        reply = blackjackSocket.getSocket().recv(0);
        blackjackgame.setItem(4, getItem(new ItemStack(Material.RED_BANNER), "Hidden Card", "card"));

        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        reply = blackjackSocket.getSocket().recv(0);
        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        reply = blackjackSocket.getSocket().recv(0);

        //Players
        blackjackgame.setItem(21, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));
        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        reply = blackjackSocket.getSocket().recv(0);
        blackjackgame.setItem(22, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));

        //Hit and Stand Buttons
        blackjackgame.setItem(15, getItem(new ItemStack(Material.LIME_WOOL), "&aHit","", "&8Draw an additional card"));
        blackjackgame.setItem(16, getItem(new ItemStack(Material.BARRIER), "&4Stand","", "&8Finish your turn"));

        player.openInventory(blackjackgame);

        this.Blackjackgame = blackjackgame;
        this.player = player;

        return true;
    }
    public boolean blackjackGamehit(Inventory blackjackgame) {
        String nextMessage = "next";
        String hitMessage = "hit";

        byte[] reply = checkReply();

            blackjackSocket.getSocket().send(hitMessage.getBytes(ZMQ.CHARSET), 0);

            int i = 3;
            reply = blackjackSocket.getSocket().recv(0);
            boolean showDealerCard = checkDealer(new String(reply, ZMQ.CHARSET));

            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            reply = blackjackSocket.getSocket().recv(0);

            while(true){
                if(!showDealerCard && i == 4){
                    blackjackgame.setItem(4, getItem(new ItemStack(Material.RED_BANNER), "Hidden Card", "card"));
                } else {
                    blackjackgame.setItem(i, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));
                }
                blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
                reply = blackjackSocket.getSocket().recv(0);
                if(new String(reply, ZMQ.CHARSET).equals("players")){
                    break;
                }
                if(new String(reply, ZMQ.CHARSET).equals("stalemate")){
                    break;
                }
                if(new String(reply, ZMQ.CHARSET).equals("loss")){
                    break;
                }
                if(new String(reply, ZMQ.CHARSET).equals("win")){
                    break;
                }
                i++;
            }
        if(new String(reply, ZMQ.CHARSET).equals("stalemate")){
            Bukkit.getLogger().info("8recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully stalemated game");
            Bukkit.broadcastMessage("Stalemate!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }
        if(new String(reply, ZMQ.CHARSET).equals("loss")){
            Bukkit.getLogger().info("9recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully lost game");
            Bukkit.broadcastMessage("You Lost!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }
        if(new String(reply, ZMQ.CHARSET).equals("win")) {
            Bukkit.getLogger().info("10recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully won game");
            Bukkit.broadcastMessage("You Won!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }

        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            reply = blackjackSocket.getSocket().recv(0);

            i = 21;
            while(true){
                blackjackgame.setItem(i, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));
                blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
                reply = blackjackSocket.getSocket().recv(0);
                i++;
                if(new String(reply, ZMQ.CHARSET).equals("decide")){
                    break;
                }
                if(new String(reply, ZMQ.CHARSET).equals("stalemate")){
                    break;
                }
                if(new String(reply, ZMQ.CHARSET).equals("loss")){
                    break;
                }
                if(new String(reply, ZMQ.CHARSET).equals("win")){
                    break;
                }
            }
            this.player.closeInventory();
            this.player.openInventory(blackjackgame);
        if(new String(reply, ZMQ.CHARSET).equals("stalemate")){
            Bukkit.getLogger().info("12recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully stalemated game");
            Bukkit.broadcastMessage("Stalemate!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }
        if(new String(reply, ZMQ.CHARSET).equals("loss")){
            Bukkit.getLogger().info("13recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully lost game");
            Bukkit.broadcastMessage("You Lost!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }
        if(new String(reply, ZMQ.CHARSET).equals("win")) {
            Bukkit.getLogger().info("14recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully won game");
            Bukkit.broadcastMessage("You Won!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }

        this.Reply = new String(reply, ZMQ.CHARSET);
        return true;
    }
    public boolean blackjackGamestand(Inventory blackjackgame) {
        String nextMessage = "next";
        String standMessage = "stand";

        byte[] reply = checkReply();

        blackjackSocket.getSocket().send(standMessage.getBytes(ZMQ.CHARSET), 0);
        Bukkit.getLogger().info("sent: " + standMessage);

        int i = 3;
        reply = blackjackSocket.getSocket().recv(0);
        Bukkit.getLogger().info("16recieved: " + new String(reply, ZMQ.CHARSET));
        boolean showDealerCard = checkDealer(new String(reply, ZMQ.CHARSET));

        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        Bukkit.getLogger().info("sent: " + nextMessage);
        reply = blackjackSocket.getSocket().recv(0);
        Bukkit.getLogger().info("17recieved: " + new String(reply, ZMQ.CHARSET));


// Dealers
        while(true){
            if(!showDealerCard && i == 4){
                blackjackgame.setItem(4, getItem(new ItemStack(Material.RED_BANNER), "Hidden Card", "card"));
            } else {
                blackjackgame.setItem(i, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));
            }
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("18recieved: " + new String(reply, ZMQ.CHARSET));
            if(new String(reply, ZMQ.CHARSET).equals("players")){
                break;
            }
            i++;
        }

        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        Bukkit.getLogger().info("sent: " + nextMessage);
        reply = blackjackSocket.getSocket().recv(0);
        Bukkit.getLogger().info("19recieved: " + new String(reply, ZMQ.CHARSET));

// Players
        i = 21;
        while(true){
            blackjackgame.setItem(i, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("20recieved: " + new String(reply, ZMQ.CHARSET));
            i++;
            if(new String(reply, ZMQ.CHARSET).equals("dealers_unhidden")){
                blackjackGameDealer(blackjackgame);
                this.player.closeInventory();
                return true;
            }
            if(new String(reply, ZMQ.CHARSET).equals("decide")){
                break;
            }
            if(new String(reply, ZMQ.CHARSET).equals("stalemate")){
                break;
            }
            if(new String(reply, ZMQ.CHARSET).equals("loss")){
                break;
            }
            if(new String(reply, ZMQ.CHARSET).equals("win")){
                break;
            }
        }
        this.player.closeInventory();
        this.player.openInventory(blackjackgame);
        if(new String(reply, ZMQ.CHARSET).equals("stalemate")){
            Bukkit.getLogger().info("21recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully stalemated game");
            Bukkit.broadcastMessage("Stalemate!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }
        if(new String(reply, ZMQ.CHARSET).equals("loss")){
            Bukkit.getLogger().info("22recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully lost game");
            Bukkit.broadcastMessage("You Lost!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }
        if(new String(reply, ZMQ.CHARSET).equals("win")) {
            Bukkit.getLogger().info("23recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully won game");
            Bukkit.broadcastMessage("You Won!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return true;
        }

        this.Reply = new String(reply, ZMQ.CHARSET);
        Bukkit.getLogger().info("24recieved: " + new String(reply, ZMQ.CHARSET));
        return true;

    }
    public void blackjackGameDealer(Inventory blackjackgame) {
        String nextMessage = "next";
        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        Bukkit.getLogger().info("sent: " + nextMessage);
        int i = 3;
        byte[] reply = blackjackSocket.getSocket().recv(0);
        Bukkit.getLogger().info("25recieved: " + new String(reply, ZMQ.CHARSET));
// Dealers
        while(true){
            blackjackgame.setItem(i, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));

            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("26recieved: " + new String(reply, ZMQ.CHARSET));
            if(new String(reply, ZMQ.CHARSET).equals("players")){
                break;
            }
            i++;
        }
        if(new String(reply, ZMQ.CHARSET).equals("stalemate")){
            Bukkit.getLogger().info("27recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully stalemated game");
            Bukkit.broadcastMessage("Stalemate!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return;
        }
        if(new String(reply, ZMQ.CHARSET).equals("loss")){
            Bukkit.getLogger().info("28recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully lost game");
            Bukkit.broadcastMessage("You Lost!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return;
        }
        if(new String(reply, ZMQ.CHARSET).equals("win")) {
            Bukkit.getLogger().info("29recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully won game");
            Bukkit.broadcastMessage("You Won!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return;
        }

        blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
        Bukkit.getLogger().info("sent: " + nextMessage);
        reply = blackjackSocket.getSocket().recv(0);
        Bukkit.getLogger().info("30recieved: " + new String(reply, ZMQ.CHARSET));
// Players
        i = 21;
        while(true){
            blackjackgame.setItem(i, getItem(new ItemStack(Material.WHITE_BANNER), new String(reply, ZMQ.CHARSET), "card"));
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("31recieved: " + new String(reply, ZMQ.CHARSET));
            i++;
            if(new String(reply, ZMQ.CHARSET).equals("loss") || new String(reply, ZMQ.CHARSET).equals("win") || new String(reply, ZMQ.CHARSET).equals("stalemate")) {
                break;
            }
            if(new String(reply, ZMQ.CHARSET).equals("dealers_unhidden")){
                blackjackGameDealer(blackjackgame);
                break;
            }
        }
        this.player.closeInventory();
        this.player.openInventory(blackjackgame);
        if(new String(reply, ZMQ.CHARSET).equals("stalemate")){
            Bukkit.getLogger().info("32recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully stalemated game");
            Bukkit.broadcastMessage("Stalemate!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));
            this.player.closeInventory();
            return;
        }
        if(new String(reply, ZMQ.CHARSET).equals("loss")){
            Bukkit.getLogger().info("33recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully lost game");
            Bukkit.broadcastMessage("You Lost!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return;
        }
        if(new String(reply, ZMQ.CHARSET).equals("win")) {
            Bukkit.getLogger().info("34recieved: " + new String(reply, ZMQ.CHARSET));
            try {
                Thread.sleep(1500);
            } catch(Exception e) {
                e.printStackTrace();
            }
            Bukkit.getLogger().info("successfully won game");
            Bukkit.broadcastMessage("You Won!");
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            reply = blackjackSocket.getSocket().recv(0);
            Bukkit.getLogger().info("F recieved: " + new String(reply, ZMQ.CHARSET));
            Bukkit.broadcastMessage("Seed: " + new String(reply, ZMQ.CHARSET));            this.player.closeInventory();
            return;
        }

        this.Reply = new String(reply, ZMQ.CHARSET);
        Bukkit.getLogger().info("35recieved: " + new String(reply, ZMQ.CHARSET));
        return;
    }
    private ItemStack getItem(ItemStack item, String name, String blockEntityTag, String ... lore) {

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lores = new ArrayList<>();
        for(String s : lore) {
            lores.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        meta.setLore(lores);

        if (meta instanceof BannerMeta && !Objects.equals(blockEntityTag, "")) {
            BannerMeta bannerMeta = (BannerMeta) meta;
            bannerMeta = makeCards(name, bannerMeta);
            item.setItemMeta(bannerMeta);
        }
        else{
            item.setItemMeta(meta);
        }
        return item;
    }
    private DyeColor getDyeColor(String suit) {
        if(Objects.equals(suit, "Spades") || Objects.equals(suit, "Clubs")) {
            return DyeColor.BLACK;
        }
        else {
            return DyeColor.RED;
        }
    }
    private BannerMeta makeCards(String name, BannerMeta bannerMeta) {
        String[] words = name.split(" ");
        if(Objects.equals(words[0], "Hidden")){
            bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_SMALL));
            bannerMeta.addPattern(new Pattern(DyeColor.RED, PatternType.CURLY_BORDER));
            bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLES_TOP));
            bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLES_BOTTOM));
            bannerMeta.addPattern(new Pattern(DyeColor.RED, PatternType.RHOMBUS_MIDDLE));
            bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
            bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.FLOWER));
            bannerMeta.addPattern(new Pattern(DyeColor.RED, PatternType.CIRCLE_MIDDLE));
            return bannerMeta;
        }
        DyeColor color = getDyeColor(words[2]);
        switch(words[0]){
            case "Two":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_TOP));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_BOTTOM));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_DOWNLEFT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Three":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_BOTTOM));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_MIDDLE));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_TOP));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.CURLY_BORDER));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_RIGHT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Four":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_LEFT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_MIRROR));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_RIGHT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_MIDDLE));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Five":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_BOTTOM));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_TOP));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_DOWNRIGHT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Six":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_BOTTOM));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_RIGHT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_MIDDLE));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_TOP));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_LEFT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Seven":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_DOWNLEFT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_TOP));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Eight":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_RIGHT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_LEFT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_TOP));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_MIDDLE));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_BOTTOM));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Nine":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_LEFT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_MIRROR));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_MIDDLE));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_TOP));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_RIGHT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_BOTTOM));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Ten":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_DOWNRIGHT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_DOWNLEFT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Jack":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_LEFT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_BOTTOM));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_RIGHT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Queen":
                bannerMeta.addPattern(new Pattern(color, PatternType.HALF_HORIZONTAL));
                bannerMeta.addPattern(new Pattern(color, PatternType.HALF_HORIZONTAL_MIRROR));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_LEFT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_RIGHT));
                bannerMeta.addPattern(new Pattern(color, PatternType.SQUARE_BOTTOM_RIGHT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "King":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_DOWNRIGHT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_DOWNLEFT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_LEFT));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
            case "Ace":
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_LEFT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_RIGHT));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_MIDDLE));
                bannerMeta.addPattern(new Pattern(color, PatternType.STRIPE_TOP));
                bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                return bannerMeta;
        }
        Bukkit.getLogger().info("Card error somehow");
        return bannerMeta;
    }
    private byte[] checkReply() {
        String nextMessage = "next";
        if(!Objects.equals(this.Reply, "decide")) {
            blackjackSocket.getSocket().send(nextMessage.getBytes(ZMQ.CHARSET), 0);
            Bukkit.getLogger().info("sent: " + nextMessage);
            return blackjackSocket.getSocket().recv(0);
        } else {
            this.Reply = "";
            return new byte[0];
        }
    }
    private boolean checkDealer(String response) {
        return Objects.equals(response, "dealers_unhidden");
    }
    private static class BlackjackSocket {
        private ZContext context;
        private ZMQ.Socket socket;

        public BlackjackSocket() {
            this.context = new ZContext();
            this.socket = context.createSocket(SocketType.REQ);
        }

        public void connect() {
            this.socket.connect("tcp://localhost:12345");
        }

        public ZMQ.Socket getSocket() {
            return this.socket;
        }
    }
}



