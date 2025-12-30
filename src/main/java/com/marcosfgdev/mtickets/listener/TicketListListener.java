package com.marcosfgdev.mtickets.listener;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.marcosfgdev.mtickets.gui.TicketListMenu;
import com.marcosfgdev.mtickets.ticket.Ticket;
import com.marcosfgdev.mtickets.ticket.TicketStatus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TicketListListener implements Listener {

    private final MTicketsPlugin plugin;
    private final TicketListMenu ticketListMenu;

    private final Map<UUID, Integer> viewingTicket = new HashMap<>();

    private final Map<UUID, Integer> respondingTo = new HashMap<>();

    public TicketListListener(MTicketsPlugin plugin) {
        this.plugin = plugin;
        this.ticketListMenu = new TicketListMenu(plugin);
    }

    public TicketListMenu getTicketListMenu() {
        return ticketListMenu;
    }

    public boolean isRespondingTo(UUID uuid) {
        return respondingTo.containsKey(uuid);
    }

    public Integer getRespondingTicketId(UUID uuid) {
        return respondingTo.remove(uuid);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getInventory().getTitle();

        if (title.startsWith(TicketListMenu.MENU_TITLE) || title.startsWith(TicketListMenu.PLAYER_TICKETS_TITLE)) {
            handleListClick(event, player, title);
            return;
        }

        if (title.startsWith(TicketListMenu.DETAIL_TITLE)) {
            handleDetailClick(event, player, title);
        }
    }

    private void handleListClick(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getRawSlot();
        boolean isPlayerList = title.startsWith(TicketListMenu.PLAYER_TICKETS_TITLE);

        int currentPage = 1;
        try {
            String pageStr = title.substring(title.indexOf("(") + 1, title.indexOf("/"));
            currentPage = Integer.parseInt(pageStr.trim());
        } catch (Exception ignored) {
        }

        if (slot >= 0 && slot < 45 && clicked.getType() == Material.PAPER) {
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("#")) {
                try {

                    String idPart = name.split(" ")[0];
                    int ticketId = Integer.parseInt(idPart.replaceAll("[^0-9]", ""));

                    Ticket ticket = plugin.getTicketManager().getTicket(ticketId);
                    if (ticket != null) {
                        viewingTicket.put(player.getUniqueId(), ticketId);
                        ticketListMenu.openDetail(player, ticket);
                    }
                } catch (Exception e) {
                    player.sendMessage("§cErro ao abrir ticket.");
                }
            }
            return;
        }

        switch (slot) {
            case 45:
                if (clicked.getType() == Material.ARROW) {
                    if (isPlayerList) {
                        ticketListMenu.openPlayerTickets(player, currentPage - 1);
                    } else {
                        ticketListMenu.openList(player, currentPage - 1);
                    }
                }
                break;
            case 47:
                if (clicked.getType() == Material.SLIME_BALL && !isPlayerList) {
                    ticketListMenu.openList(player, currentPage);
                    player.sendMessage(plugin.getMessage("prefix") + "§aLista atualizada!");
                }
                break;
            case 49:
                if (isPlayerList && clicked.getType() == Material.BARRIER) {
                    new com.marcosfgdev.mtickets.gui.TicketMenu(plugin).open(player);
                }
                break;
            case 51:
                if (!isPlayerList && clicked.getType() == Material.BARRIER) {
                    player.closeInventory();
                }
                break;
            case 53:
                if (clicked.getType() == Material.ARROW) {
                    if (isPlayerList) {
                        ticketListMenu.openPlayerTickets(player, currentPage + 1);
                    } else {
                        ticketListMenu.openList(player, currentPage + 1);
                    }
                }
                break;
        }
    }

    private void handleDetailClick(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getRawSlot();

        int ticketId;
        try {
            ticketId = Integer.parseInt(title.replace(TicketListMenu.DETAIL_TITLE, "").trim());
        } catch (Exception e) {
            return;
        }

        Ticket ticket = plugin.getTicketManager().getTicket(ticketId);
        if (ticket == null) {
            player.closeInventory();
            player.sendMessage(plugin.getMessage("ticket-not-found"));
            return;
        }

        switch (slot) {
            case 38:
                if (clicked.getType() == Material.SIGN) {
                    player.closeInventory();
                    respondingTo.put(player.getUniqueId(), ticketId);
                    player.sendMessage("");
                    player.sendMessage("§a§lResponder Ticket #" + ticketId);
                    player.sendMessage("§7Digite sua resposta no chat.");
                    player.sendMessage("§7Digite '§ccancelar§7' para desistir.");
                    player.sendMessage("");
                }
                break;

            case 40:
                if (clicked.getType() == Material.NAME_TAG) {
                    ticket.setAssignedStaff(player.getName());
                    ticket.setStatus(TicketStatus.IN_PROGRESS);
                    plugin.getTicketDatabase().getTicketRepository().updateTicket(ticket);
                    player.sendMessage(plugin.getMessage("prefix") + "§aVoce assumiu o ticket #" + ticketId);
                    ticketListMenu.openDetail(player, ticket);
                }
                break;

            case 42:
                if (clicked.getType() == Material.REDSTONE) {
                    plugin.getTicketManager().closeTicket(ticketId, player.getName());
                    player.closeInventory();
                    player.sendMessage(plugin.getMessage("ticket-closed").replace("%id%", String.valueOf(ticketId)));
                }
                break;

            case 45:
                if (clicked.getType() == Material.ARROW) {
                    viewingTicket.remove(player.getUniqueId());
                    if (player.hasPermission("mtickets.admin")) {
                        ticketListMenu.openList(player, 1);
                    } else {
                        ticketListMenu.openPlayerTickets(player, 1);
                    }
                }
                break;
        }
    }
}
