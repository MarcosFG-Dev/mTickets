package com.marcosfgdev.mtickets.gui;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TicketMenu {

    public static final String MENU_TITLE_ID = "§8§lAbrir Ticket";

    private final MTicketsPlugin plugin;

    public TicketMenu(MTicketsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = plugin.getRawMessage("menu-title");
        if (title == null || title.isEmpty()) {
            title = MENU_TITLE_ID;
        }

        Inventory inv = Bukkit.createInventory(null, 27, MTicketsPlugin.colorize(title));

        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short) 7, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        ItemStack createItem = new ItemStack(Material.BOOK_AND_QUILL);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.setDisplayName("§a§lAbrir Novo Ticket");
        List<String> createLore = new ArrayList<>();
        createLore.add("§7Clique para solicitar ajuda");
        createLore.add("§7da nossa equipe.");
        createMeta.setLore(createLore);
        createItem.setItemMeta(createMeta);
        inv.setItem(11, createItem);

        ItemStack myTicketsItem = new ItemStack(Material.PAPER);
        ItemMeta myTicketsMeta = myTicketsItem.getItemMeta();
        myTicketsMeta.setDisplayName("§e§lMeus Tickets");
        List<String> myTicketsLore = new ArrayList<>();
        myTicketsLore.add("§7Veja seus tickets antigos");
        myTicketsLore.add("§7e responda as mensagens.");
        myTicketsMeta.setLore(myTicketsLore);
        myTicketsItem.setItemMeta(myTicketsMeta);
        inv.setItem(15, myTicketsItem);

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, short data, String name) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MTicketsPlugin.colorize(name));
        item.setItemMeta(meta);
        return item;
    }
}
