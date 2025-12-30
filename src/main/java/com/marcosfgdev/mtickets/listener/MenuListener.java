package com.marcosfgdev.mtickets.listener;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.marcosfgdev.mtickets.gui.TicketMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    private final MTicketsPlugin plugin;

    public MenuListener(MTicketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getInventory().getTitle();

        String menuTitle = plugin.getRawMessage("menu-title");
        if (menuTitle == null || menuTitle.isEmpty()) {
            menuTitle = TicketMenu.MENU_TITLE_ID;
        }

        if (!title.equals(MTicketsPlugin.colorize(menuTitle))) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        if (event.getRawSlot() == 11) {
            player.closeInventory();

            if (!plugin.getTicketManager().canCreateTicket(player.getUniqueId())) {
                long cooldown = plugin.getTicketManager().getRemainingCooldown(player.getUniqueId());
                if (cooldown > 0) {
                    String msg = plugin.getMessage("cooldown").replace("%time%", String.valueOf(cooldown));
                    player.sendMessage(msg);
                } else {
                    player.sendMessage(plugin.getMessage("already-pending"));
                }
                return;
            }

            plugin.getTicketManager().setAwaitingMessage(player.getUniqueId(), true);
            player.sendMessage(plugin.getMessage("chat-prompt"));
            return;
        }

        if (event.getRawSlot() == 15) {
            player.closeInventory();
            new com.marcosfgdev.mtickets.gui.TicketListMenu(plugin).openPlayerTickets(player, 1);
            return;
        }
    }
}
