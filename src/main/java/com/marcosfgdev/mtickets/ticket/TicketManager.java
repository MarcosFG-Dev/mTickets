package com.marcosfgdev.mtickets.ticket;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.marcosfgdev.mtickets.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TicketManager {

    private final MTicketsPlugin plugin;
    private final Database database;

    private final Map<Integer, Ticket> ticketCache;

    private final Set<UUID> awaitingMessage;

    private final Map<UUID, Long> cooldowns;

    public TicketManager(MTicketsPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.ticketCache = new ConcurrentHashMap<>();
        this.awaitingMessage = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.cooldowns = new ConcurrentHashMap<>();

        loadOpenTickets();
    }

    private void loadOpenTickets() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Ticket> openTickets = database.getTicketRepository().getOpenTickets();
            for (Ticket ticket : openTickets) {
                ticketCache.put(ticket.getId(), ticket);
            }
            plugin.getLogger().info("Carregados " + openTickets.size() + " tickets abertos.");
        });
    }

    public Ticket createTicket(Player player, String message) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        Ticket ticket = new Ticket(uuid, name, message);

        int id = database.getTicketRepository().createTicket(ticket);
        ticket.setId(id);

        ticketCache.put(id, ticket);

        int cooldownTime = plugin.getConfig().getInt("settings.ticket-cooldown", 300);
        cooldowns.put(uuid, System.currentTimeMillis() + (cooldownTime * 1000L));

        notifyStaff(ticket);

        if (plugin.getDiscordBotService() != null) {
            plugin.getDiscordBotService().createTicketChannel(ticket);
        }

        playSound(player, "sound-on-create");

        return ticket;
    }

    public void addReply(int ticketId, String author, String authorType, String message) {
        addReply(getTicket(ticketId), author, authorType, message, false);
    }

    public void addReply(Ticket ticket, String author, String authorType, String message, boolean fromDiscord) {
        if (ticket == null || !ticket.isOpen())
            return;

        Ticket.TicketReply reply = new Ticket.TicketReply(ticket.getId(), author, authorType, message);

        database.getTicketRepository().addReply(reply);

        ticket.addReply(reply);

        if (!fromDiscord && plugin.getDiscordBotService() != null) {
            plugin.getDiscordBotService().sendTicketMessage(ticket, author, message);
        }

        if ("STAFF".equals(authorType)) {

            if (ticket.getAssignedStaff() == null) {
                ticket.setAssignedStaff(author);
            }

            ticket.setStatus(TicketStatus.WAITING_REPLY);
            database.getTicketRepository().updateTicket(ticket);

            Player player = Bukkit.getPlayer(ticket.getPlayerUUID());
            if (player != null && player.isOnline()) {
                String msg = plugin.getConfig().getString("messages.staff-reply-format",
                        "&8[&bSTAFF&8] &f%staff%&7: &f%message%");

                if (!msg.contains("%message%"))
                    msg += " &f%message%";

                msg = msg.replace("%staff%", author).replace("%message%", message);
                player.sendMessage("");
                player.sendMessage(MTicketsPlugin.colorize(msg));
                player.sendMessage(MTicketsPlugin.colorize("&eAbra o menu /ticket para responder."));
                player.sendMessage("");
                playSound(player, "sound-on-reply");
            }
        } else {

            ticket.setStatus(TicketStatus.IN_PROGRESS);
            database.getTicketRepository().updateTicket(ticket);

            String alertMsg = plugin.getRawMessage("player-reply-alert");
            if (alertMsg == null || alertMsg.isEmpty())
                alertMsg = "&e[Ticket #%id%] &f%player% respondeu: &f%message%";

            if (!alertMsg.contains("%message%"))
                alertMsg += " &8> &7%message%";

            alertMsg = alertMsg.replace("%id%", String.valueOf(ticket.getId()))
                    .replace("%player%", author)
                    .replace("%message%", message);

            Set<Player> targets = new HashSet<>();

            if (ticket.getAssignedStaff() != null) {

                Player owner = Bukkit.getPlayer(ticket.getAssignedStaff());
                if (owner != null && owner.isOnline()) {
                    targets.add(owner);
                } else {

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission("mtickets.notify"))
                            targets.add(p);
                    }
                }
            } else {

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("mtickets.notify"))
                        targets.add(p);
                }
            }

            for (Player p : targets) {
                p.sendMessage(MTicketsPlugin.colorize(alertMsg));
                playSound(p, "sound-on-reply");
            }
        }
    }

    public void closeTicket(int ticketId, String closedBy) {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null)
            return;

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setAssignedStaff(closedBy);

        database.getTicketRepository().updateTicket(ticket);

        // Discord Hook: Fechar Canal
        if (plugin.getDiscordBotService() != null) {
            plugin.getDiscordBotService().closeTicketChannel(ticket);
        }

        ticketCache.remove(ticketId);
    }

    public Ticket getTicket(int ticketId) {

        Ticket cached = ticketCache.get(ticketId);
        if (cached != null)
            return cached;

        Ticket ticket = database.getTicketRepository().getTicketById(ticketId);
        if (ticket != null && ticket.isOpen()) {
            ticketCache.put(ticketId, ticket);
        }
        return ticket;
    }

    public List<Ticket> getOpenTickets() {
        return new ArrayList<>(ticketCache.values());
    }

    public List<Ticket> getPlayerTickets(UUID playerUUID) {
        return database.getTicketRepository().getTicketsByPlayer(playerUUID);
    }

    public int getOpenTicketCount(UUID playerUUID) {
        int count = 0;
        for (Ticket ticket : ticketCache.values()) {
            if (ticket.getPlayerUUID().equals(playerUUID) && ticket.isOpen()) {
                count++;
            }
        }
        return count;
    }

    public boolean canCreateTicket(UUID playerUUID) {

        int maxTickets = plugin.getConfig().getInt("settings.max-open-tickets", 3);
        if (getOpenTicketCount(playerUUID) >= maxTickets) {
            return false;
        }

        Long cooldownEnd = cooldowns.get(playerUUID);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            return false;
        }

        return true;
    }

    public long getRemainingCooldown(UUID playerUUID) {
        Long cooldownEnd = cooldowns.get(playerUUID);
        if (cooldownEnd == null)
            return 0;
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    public void setAwaitingMessage(UUID uuid, boolean awaiting) {
        if (awaiting) {
            awaitingMessage.add(uuid);

            int timeout = plugin.getConfig().getInt("settings.chat-timeout", 60);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (awaitingMessage.remove(uuid)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(plugin.getMessage("chat-timeout"));
                        });
                    }
                }
            }, timeout * 20L);
        } else {
            awaitingMessage.remove(uuid);
        }
    }

    public boolean isAwaitingMessage(UUID uuid) {
        return awaitingMessage.contains(uuid);
    }

    private void notifyStaff(Ticket ticket) {
        if (!plugin.getConfig().getBoolean("settings.notify-staff", true))
            return;

        String alertMsg = plugin.getRawMessage("new-ticket-alert")
                .replace("%player%", ticket.getPlayerName())
                .replace("%id%", String.valueOf(ticket.getId()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("mtickets.notify")) {
                player.sendMessage(MTicketsPlugin.colorize(alertMsg));
                playSound(player, "sound-on-create");
            }
        }
    }

    private void playSound(Player player, String configKey) {
        try {
            String soundName = plugin.getConfig().getString("settings." + configKey, "NOTE_PLING");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {

        }
    }

    public List<Ticket> getAllTickets(int page, int perPage) {
        return database.getTicketRepository().getAllTickets(page, perPage);
    }

    public int getTotalTicketCount() {
        return database.getTicketRepository().getTotalCount();
    }
}
