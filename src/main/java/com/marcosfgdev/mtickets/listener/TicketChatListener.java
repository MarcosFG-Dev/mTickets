package com.marcosfgdev.mtickets.listener;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.marcosfgdev.mtickets.ticket.Ticket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class TicketChatListener implements Listener {

    private final MTicketsPlugin plugin;

    public TicketChatListener(MTicketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        TicketListListener listListener = plugin.getTicketListListener();
        if (listListener != null && listListener.isRespondingTo(player.getUniqueId())) {
            event.setCancelled(true);

            if (message.equalsIgnoreCase("cancelar") || message.equalsIgnoreCase("cancel")) {
                listListener.getRespondingTicketId(player.getUniqueId());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(plugin.getMessage("chat-cancelled"));
                });
                return;
            }

            Integer ticketId = listListener.getRespondingTicketId(player.getUniqueId());
            if (ticketId != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String authorType = player.hasPermission("mtickets.staff") ? "STAFF" : "PLAYER";
                    plugin.getTicketManager().addReply(ticketId, player.getName(), authorType, message);
                    player.sendMessage(plugin.getMessage("ticket-replied").replace("%id%", String.valueOf(ticketId)));

                    Ticket updatedTicket = plugin.getTicketManager().getTicket(ticketId);
                    if (updatedTicket != null) {
                        listListener.getTicketListMenu().openDetail(player, updatedTicket);
                    }
                });
            }
            return;
        }

        if (!plugin.getTicketManager().isAwaitingMessage(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        if (message.equalsIgnoreCase("cancelar") || message.equalsIgnoreCase("cancel")) {
            plugin.getTicketManager().setAwaitingMessage(player.getUniqueId(), false);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.getMessage("chat-cancelled"));
            });
            return;
        }

        plugin.getTicketManager().setAwaitingMessage(player.getUniqueId(), false);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Ticket ticket = plugin.getTicketManager().createTicket(player, message);
            if (ticket != null) {
                String msg = plugin.getMessage("ticket-opened").replace("%id%", String.valueOf(ticket.getId()));
                player.sendMessage(msg);
            }
        });
    }
}
