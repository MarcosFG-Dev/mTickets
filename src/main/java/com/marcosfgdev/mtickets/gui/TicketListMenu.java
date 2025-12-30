package com.marcosfgdev.mtickets.gui;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.marcosfgdev.mtickets.ticket.Ticket;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TicketListMenu {

    public static final String MENU_TITLE = "§8§lTickets Abertos";
    public static final String PLAYER_TICKETS_TITLE = "§8§lMeus Tickets";
    public static final String DETAIL_TITLE = "§8§lTicket #";

    private final MTicketsPlugin plugin;

    public TicketListMenu(MTicketsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre o menu de lista de tickets
     */
    public void openList(Player player, int page) {
        List<Ticket> allTickets = plugin.getTicketManager().getOpenTickets();
        int totalPages = (int) Math.ceil(allTickets.size() / 45.0);
        if (totalPages == 0)
            totalPages = 1;
        if (page < 1)
            page = 1;
        if (page > totalPages)
            page = totalPages;

        String title = MENU_TITLE + " §7(" + page + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, allTickets.size());

        for (int i = startIndex; i < endIndex; i++) {
            Ticket ticket = allTickets.get(i);
            ItemStack item = createTicketItem(ticket);
            inv.setItem(i - startIndex, item);
        }

        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short) 7, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        if (page > 1) {
            ItemStack prev = createItem(Material.ARROW, (short) 0, "§a« Pagina Anterior");
            ItemMeta meta = prev.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add("§7Clique para ir para a pagina " + (page - 1));
            meta.setLore(lore);
            prev.setItemMeta(meta);
            inv.setItem(45, prev);
        }

        ItemStack info = createItem(Material.PAPER, (short) 0, "§e§lInformacoes");
        ItemMeta infoMeta = info.getItemMeta();
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7");
        infoLore.add("§fTotal de tickets: §a" + allTickets.size());
        infoLore.add("§fPagina: §e" + page + "§7/§e" + totalPages);
        infoLore.add("§7");
        infoLore.add("§7Clique em um ticket para");
        infoLore.add("§7ver detalhes e responder.");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        if (page < totalPages) {
            ItemStack next = createItem(Material.ARROW, (short) 0, "§aProxima Pagina »");
            ItemMeta meta = next.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add("§7Clique para ir para a pagina " + (page + 1));
            meta.setLore(lore);
            next.setItemMeta(meta);
            inv.setItem(53, next);
        }

        ItemStack refresh = createItem(Material.SLIME_BALL, (short) 0, "§b§lAtualizar");
        ItemMeta refreshMeta = refresh.getItemMeta();
        List<String> refreshLore = new ArrayList<>();
        refreshLore.add("§7Clique para atualizar a lista");
        refreshMeta.setLore(refreshLore);
        refresh.setItemMeta(refreshMeta);
        inv.setItem(47, refresh);

        ItemStack close = createItem(Material.BARRIER, (short) 0, "§c§lFechar");
        inv.setItem(51, close);

        player.openInventory(inv);
    }

    /**
     * Abre o menu de tickets do jogador
     */
    public void openPlayerTickets(Player player, int page) {
        List<Ticket> allTickets = plugin.getTicketDatabase().getTicketRepository()
                .getTicketsByPlayer(player.getUniqueId());
        int totalPages = (int) Math.ceil(allTickets.size() / 45.0);
        if (totalPages == 0)
            totalPages = 1;
        if (page < 1)
            page = 1;
        if (page > totalPages)
            page = totalPages;

        String title = PLAYER_TICKETS_TITLE + " §7(" + page + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, allTickets.size());

        for (int i = startIndex; i < endIndex; i++) {
            Ticket ticket = allTickets.get(i);
            ItemStack item = createTicketItem(ticket);
            inv.setItem(i - startIndex, item);
        }

        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short) 7, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        if (page > 1) {
            ItemStack prev = createItem(Material.ARROW, (short) 0, "§a« Pagina Anterior");
            inv.setItem(45, prev);
        }

        if (page < totalPages) {
            ItemStack next = createItem(Material.ARROW, (short) 0, "§aProxima Pagina »");
            inv.setItem(53, next);
        }

        ItemStack back = createItem(Material.BARRIER, (short) 0, "§c§lVoltar");
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    /**
     * Abre o menu de detalhes de um ticket
     */
    public void openDetail(Player player, Ticket ticket) {
        String title = DETAIL_TITLE + ticket.getId();
        Inventory inv = Bukkit.createInventory(null, 54, title);
        boolean isAdmin = player.hasPermission("mtickets.admin");

        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short) 7, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ItemStack skull = SkullUtil.getPlayerSkull(ticket.getPlayerName());
        ItemMeta skullMeta = skull.getItemMeta();
        skullMeta.setDisplayName("§e§l" + ticket.getPlayerName());
        List<String> skullLore = new ArrayList<>();
        skullLore.add("§7");
        skullLore.add("§fStatus: " + getStatusColor(ticket.getStatus().name()) + ticket.getStatus().getDisplayName());
        skullLore.add("§fCriado: §7" + formatDate(ticket.getCreatedAt()));
        skullLore.add("§7");
        skullMeta.setLore(skullLore);
        skull.setItemMeta(skullMeta);
        inv.setItem(4, skull);

        ItemStack message = createItem(Material.PAPER, (short) 0, "§f§lMensagem");
        ItemMeta msgMeta = message.getItemMeta();
        List<String> msgLore = new ArrayList<>();
        msgLore.add("§7");
        String[] words = ticket.getMessage().split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() > 40) {
                msgLore.add("§f" + line.toString().trim());
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (line.length() > 0)
            msgLore.add("§f" + line.toString().trim());
        msgLore.add("§7");
        msgMeta.setLore(msgLore);
        message.setItemMeta(msgMeta);
        inv.setItem(20, message);

        ItemStack replies = createItem(Material.BOOK, (short) 0,
                "§a§lRespostas §7(" + ticket.getReplies().size() + ")");
        ItemMeta repliesMeta = replies.getItemMeta();
        List<String> repliesLore = new ArrayList<>();
        repliesLore.add("§7");
        if (ticket.getReplies().isEmpty()) {
            repliesLore.add("§7Nenhuma resposta ainda.");
        } else {
            int count = 0;
            for (Ticket.TicketReply reply : ticket.getReplies()) {
                if (count >= 5) {
                    repliesLore.add("§8... e mais " + (ticket.getReplies().size() - 5) + " respostas");
                    break;
                }

                String prefix;
                if (reply.getAuthor().equals(player.getName())) {
                    prefix = "§e[Você]";
                } else if ("STAFF".equals(reply.getAuthorType())) {
                    prefix = "§b[Staff]";
                } else {
                    prefix = "§a[Player]";
                }

                String msg = reply.getMessage();
                if (msg.length() > 30)
                    msg = msg.substring(0, 30) + "...";
                repliesLore.add(prefix + " §f" + reply.getAuthor() + "§7: " + msg);
                count++;
            }
        }
        repliesLore.add("§7");
        repliesMeta.setLore(repliesLore);
        replies.setItemMeta(repliesMeta);
        inv.setItem(22, replies);

        ItemStack status = createItem(getStatusMaterial(ticket.getStatus().name()),
                getStatusData(ticket.getStatus().name()),
                getStatusColor(ticket.getStatus().name()) + "§l" + ticket.getStatus().getDisplayName());
        ItemMeta statusMeta = status.getItemMeta();
        List<String> statusLore = new ArrayList<>();
        statusLore.add("§7");
        statusLore.add("§7Ultima atualizacao:");
        statusLore.add("§f" + formatDate(ticket.getUpdatedAt()));
        statusLore.add("§7");
        statusMeta.setLore(statusLore);
        status.setItemMeta(statusMeta);
        inv.setItem(24, status);

        if (ticket.isOpen()) {

            ItemStack respond = createItem(Material.SIGN, (short) 0, "§a§lResponder");
            ItemMeta respondMeta = respond.getItemMeta();
            List<String> respondLore = new ArrayList<>();
            respondLore.add("§7");
            respondLore.add("§7Clique para responder.");
            respondLore.add("§7Digite a mensagem no chat.");
            respondLore.add("§7");
            respondMeta.setLore(respondLore);
            respond.setItemMeta(respondMeta);
            inv.setItem(38, respond);
        } else {

            ItemStack locked = createItem(Material.BARRIER, (short) 0, "§c§lTicket Fechado");
            ItemMeta lockedMeta = locked.getItemMeta();
            List<String> lockedLore = new ArrayList<>();
            lockedLore.add("§7");
            lockedLore.add("§7Este ticket ja foi encerrado.");
            lockedLore.add("§7Nao e possivel responder.");
            lockedLore.add("§7");
            lockedMeta.setLore(lockedLore);
            locked.setItemMeta(lockedMeta);
            inv.setItem(38, locked);
        }

        if (isAdmin) {

            ItemStack assign = createItem(Material.NAME_TAG, (short) 0, "§e§lAssumir Ticket");
            ItemMeta assignMeta = assign.getItemMeta();
            List<String> assignLore = new ArrayList<>();
            assignLore.add("§7");
            if (ticket.getAssignedStaff() != null) {
                assignLore.add("§7Atribuido a: §f" + ticket.getAssignedStaff());
            } else {
                assignLore.add("§7Clique para assumir este ticket.");
            }
            assignLore.add("§7");
            assignMeta.setLore(assignLore);
            assign.setItemMeta(assignMeta);
            inv.setItem(40, assign);

            ItemStack closeTicket = createItem(Material.REDSTONE, (short) 0, "§c§lFechar Ticket");
            ItemMeta closeMeta = closeTicket.getItemMeta();
            List<String> closeLore = new ArrayList<>();
            closeLore.add("§7");
            closeLore.add("§7Clique para fechar o ticket.");
            closeLore.add("§cIsso notificara o jogador.");
            closeLore.add("§7");
            closeMeta.setLore(closeLore);
            closeTicket.setItemMeta(closeMeta);
            inv.setItem(42, closeTicket);
        }

        ItemStack back = createItem(Material.ARROW, (short) 0, "§7§lVoltar");
        inv.setItem(45, back);

        player.openInventory(inv);
    }

    private ItemStack createTicketItem(Ticket ticket) {
        ItemStack item = createItem(Material.PAPER, (short) 0,
                "§a§l#" + ticket.getId() + " §7- §f" + ticket.getPlayerName());
        ItemMeta meta = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§fStatus: " + getStatusColor(ticket.getStatus().name()) + ticket.getStatus().getDisplayName());
        lore.add("§fRespostas: §e" + ticket.getReplies().size());
        lore.add("§7");

        String msg = ticket.getMessage();
        if (msg.length() > 40) {
            msg = msg.substring(0, 40) + "...";
        }
        lore.add("§f" + msg);
        lore.add("§7");
        lore.add("§eClique para ver detalhes");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, short data, String name) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "OPEN":
                return "§a";
            case "IN_PROGRESS":
                return "§e";
            case "WAITING_REPLY":
                return "§6";
            case "CLOSED":
                return "§c";
            case "RESOLVED":
                return "§2";
            default:
                return "§7";
        }
    }

    private Material getStatusMaterial(String status) {
        switch (status) {
            case "OPEN":
                return Material.WOOL;
            case "IN_PROGRESS":
                return Material.WOOL;
            case "WAITING_REPLY":
                return Material.WOOL;
            case "CLOSED":
                return Material.WOOL;
            default:
                return Material.WOOL;
        }
    }

    private short getStatusData(String status) {
        switch (status) {
            case "OPEN":
                return 5;
            case "IN_PROGRESS":
                return 4;
            case "WAITING_REPLY":
                return 1;
            case "CLOSED":
                return 14;
            default:
                return 0;
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(new Date(timestamp));
    }
}
