package com.marcosfgdev.mtickets.storage;

import com.marcosfgdev.mtickets.ticket.Ticket;
import com.marcosfgdev.mtickets.ticket.TicketStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketRepository {

    private final Database database;

    public TicketRepository(Database database) {
        this.database = database;
    }

    public void createTables() throws Exception {
        String ticketsTable = "CREATE TABLE IF NOT EXISTS tickets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "message TEXT NOT NULL," +
                "status VARCHAR(20) NOT NULL DEFAULT 'OPEN'," +
                "assigned_staff VARCHAR(64)," +
                "created_at BIGINT NOT NULL," +
                "updated_at BIGINT NOT NULL" +
                ")";

        String repliesTable = "CREATE TABLE IF NOT EXISTS ticket_replies (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ticket_id INTEGER NOT NULL," +
                "author VARCHAR(64) NOT NULL," +
                "author_type VARCHAR(20) NOT NULL," +
                "message TEXT NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "FOREIGN KEY (ticket_id) REFERENCES tickets(id)" +
                ")";

        try (Connection conn = database.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(ticketsTable);
            stmt.execute(repliesTable);
        }
    }

    public int createTicket(Ticket ticket) {
        String sql = "INSERT INTO tickets (player_uuid, player_name, message, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, ticket.getPlayerUUID().toString());
            stmt.setString(2, ticket.getPlayerName());
            stmt.setString(3, ticket.getMessage());
            stmt.setString(4, ticket.getStatus().name());
            stmt.setLong(5, ticket.getCreatedAt());
            stmt.setLong(6, ticket.getUpdatedAt());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void updateTicket(Ticket ticket) {
        String sql = "UPDATE tickets SET status = ?, assigned_staff = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ticket.getStatus().name());
            stmt.setString(2, ticket.getAssignedStaff());
            stmt.setLong(3, ticket.getUpdatedAt());
            stmt.setInt(4, ticket.getId());

            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addReply(Ticket.TicketReply reply) {
        String sql = "INSERT INTO ticket_replies (ticket_id, author, author_type, message, created_at) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reply.getTicketId());
            stmt.setString(2, reply.getAuthor());
            stmt.setString(3, reply.getAuthorType());
            stmt.setString(4, reply.getMessage());
            stmt.setLong(5, reply.getCreatedAt());

            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Ticket getTicketById(int id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Ticket ticket = mapTicket(rs);
                ticket.setReplies(getRepliesByTicketId(id));
                return ticket;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Ticket> getOpenTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE status IN ('OPEN', 'IN_PROGRESS', 'WAITING_REPLY') ORDER BY created_at DESC";

        try (Connection conn = database.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Ticket ticket = mapTicket(rs);
                ticket.setReplies(getRepliesByTicketId(ticket.getId()));
                tickets.add(ticket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tickets;
    }

    public List<Ticket> getTicketsByPlayer(UUID playerUUID) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE player_uuid = ? ORDER BY created_at DESC";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Ticket ticket = mapTicket(rs);
                ticket.setReplies(getRepliesByTicketId(ticket.getId()));
                tickets.add(ticket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tickets;
    }

    public List<Ticket> getAllTickets(int page, int perPage) {
        List<Ticket> tickets = new ArrayList<>();
        int offset = (page - 1) * perPage;
        String sql = "SELECT * FROM tickets ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, perPage);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Ticket ticket = mapTicket(rs);
                ticket.setReplies(getRepliesByTicketId(ticket.getId()));
                tickets.add(ticket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tickets;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM tickets";

        try (Connection conn = database.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private List<Ticket.TicketReply> getRepliesByTicketId(int ticketId) {
        List<Ticket.TicketReply> replies = new ArrayList<>();
        String sql = "SELECT * FROM ticket_replies WHERE ticket_id = ? ORDER BY created_at ASC";

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticketId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Ticket.TicketReply reply = new Ticket.TicketReply();
                reply.setId(rs.getInt("id"));
                reply.setTicketId(rs.getInt("ticket_id"));
                reply.setAuthor(rs.getString("author"));
                reply.setAuthorType(rs.getString("author_type"));
                reply.setMessage(rs.getString("message"));
                reply.setCreatedAt(rs.getLong("created_at"));
                replies.add(reply);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return replies;
    }

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getInt("id"));
        ticket.setPlayerUUID(UUID.fromString(rs.getString("player_uuid")));
        ticket.setPlayerName(rs.getString("player_name"));
        ticket.setMessage(rs.getString("message"));
        ticket.setStatus(TicketStatus.valueOf(rs.getString("status")));
        ticket.setAssignedStaff(rs.getString("assigned_staff"));
        ticket.setCreatedAt(rs.getLong("created_at"));
        ticket.setUpdatedAt(rs.getLong("updated_at"));
        return ticket;
    }
}
