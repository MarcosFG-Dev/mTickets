package com.marcosfgdev.mtickets.command;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.marcosfgdev.mtickets.gui.TicketMenu;
import com.marcosfgdev.mtickets.ticket.Ticket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TicketCommand implements CommandExecutor {

    private final MTicketsPlugin plugin;
    private final TicketMenu ticketMenu;

    public TicketCommand(MTicketsPlugin plugin) {
        this.plugin = plugin;
        this.ticketMenu = new TicketMenu(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("§cApenas jogadores podem usar este comando.");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("mtickets.use")) {
                player.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }

            ticketMenu.open(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "admin":
            case "listar":
            case "lista":
                return handleList(sender);

            case "responder":
            case "reply":
                return handleReply(sender, args);

            case "fechar":
            case "close":
                return handleClose(sender, args);

            case "ver":
            case "view":
                return handleView(sender, args);

            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("mtickets.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getTicketListListener().getTicketListMenu().openList(player, 1);
            return true;
        }

        List<Ticket> tickets = plugin.getTicketManager().getOpenTickets();

        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§b§lTickets Abertos §7(" + tickets.size() + ")");
        sender.sendMessage("§8§m----------------------------------------");

        if (tickets.isEmpty()) {
            sender.sendMessage("§7Nenhum ticket aberto no momento.");
        } else {
            for (Ticket ticket : tickets) {
                sender.sendMessage(String.format(
                        "§a#%d §7- §f%s §7- %s §7- §e%s",
                        ticket.getId(),
                        ticket.getPlayerName(),
                        ticket.getStatus().getColoredName(),
                        truncate(ticket.getMessage(), 30)));
            }
        }

        sender.sendMessage("§8§m----------------------------------------");
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mtickets.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUso: /ticket responder <id> <mensagem>");
            return true;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cID invalido.");
            return true;
        }

        Ticket ticket = plugin.getTicketManager().getTicket(ticketId);
        if (ticket == null) {
            sender.sendMessage(plugin.getMessage("ticket-not-found"));
            return true;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            message.append(args[i]).append(" ");
        }

        plugin.getTicketManager().addReply(ticketId, sender.getName(), "STAFF", message.toString().trim());
        sender.sendMessage(plugin.getMessage("ticket-replied").replace("%id%", String.valueOf(ticketId)));

        return true;
    }

    private boolean handleClose(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mtickets.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /ticket fechar <id>");
            return true;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cID invalido.");
            return true;
        }

        Ticket ticket = plugin.getTicketManager().getTicket(ticketId);
        if (ticket == null) {
            sender.sendMessage(plugin.getMessage("ticket-not-found"));
            return true;
        }

        plugin.getTicketManager().closeTicket(ticketId, sender.getName());
        sender.sendMessage(plugin.getMessage("ticket-closed").replace("%id%", String.valueOf(ticketId)));

        return true;
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mtickets.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /ticket ver <id>");
            return true;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cID invalido.");
            return true;
        }

        Ticket ticket = plugin.getTicketManager().getTicket(ticketId);
        if (ticket == null) {
            sender.sendMessage(plugin.getMessage("ticket-not-found"));
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getTicketListListener().getTicketListMenu().openDetail(player, ticket);
            return true;
        }

        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§b§lTicket #" + ticket.getId());
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§7Jogador: §f" + ticket.getPlayerName());
        sender.sendMessage("§7Status: " + ticket.getStatus().getColoredName());
        sender.sendMessage("§7Mensagem: §f" + ticket.getMessage());
        sender.sendMessage("§8§m----------------------------------------");

        if (!ticket.getReplies().isEmpty()) {
            sender.sendMessage("§e§lRespostas:");
            for (Ticket.TicketReply reply : ticket.getReplies()) {
                String prefix = "STAFF".equals(reply.getAuthorType()) ? "§b[STAFF]" : "§a[PLAYER]";
                sender.sendMessage(prefix + " §f" + reply.getAuthor() + "§7: " + reply.getMessage());
            }
        }

        sender.sendMessage("§8§m----------------------------------------");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§b§lmTickets §7- Comandos");
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§e/ticket §7- Abre o menu de tickets");
        if (sender.hasPermission("mtickets.admin")) {
            sender.sendMessage("§e/ticket listar §7- Abre GUI com tickets");
            sender.sendMessage("§e/ticket ver <id> §7- Ver detalhes do ticket");
            sender.sendMessage("§e/ticket responder <id> <msg> §7- Responder");
            sender.sendMessage("§e/ticket fechar <id> §7- Fechar ticket");
        }
        sender.sendMessage("§8§m----------------------------------------");
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength) + "...";
    }
}
