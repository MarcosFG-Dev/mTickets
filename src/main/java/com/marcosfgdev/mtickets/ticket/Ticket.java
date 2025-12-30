package com.marcosfgdev.mtickets.ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ticket {

    private int id;
    private UUID playerUUID;
    private String playerName;
    private String message;
    private TicketStatus status;
    private long createdAt;
    private long updatedAt;
    private String assignedStaff;
    private List<TicketReply> replies;

    public Ticket() {
        this.replies = new ArrayList<>();
        this.status = TicketStatus.OPEN;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Ticket(UUID playerUUID, String playerName, String message) {
        this();
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(String assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    public List<TicketReply> getReplies() {
        return replies;
    }

    public void setReplies(List<TicketReply> replies) {
        this.replies = replies;
    }

    public void addReply(TicketReply reply) {
        this.replies.add(reply);
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isOpen() {
        return status == TicketStatus.OPEN || status == TicketStatus.IN_PROGRESS
                || status == TicketStatus.WAITING_REPLY;
    }

    public static class TicketReply {
        private int id;
        private int ticketId;
        private String author;
        private String authorType;
        private String message;
        private long createdAt;

        public TicketReply() {
            this.createdAt = System.currentTimeMillis();
        }

        public TicketReply(int ticketId, String author, String authorType, String message) {
            this();
            this.ticketId = ticketId;
            this.author = author;
            this.authorType = authorType;
            this.message = message;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getTicketId() {
            return ticketId;
        }

        public void setTicketId(int ticketId) {
            this.ticketId = ticketId;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getAuthorType() {
            return authorType;
        }

        public void setAuthorType(String authorType) {
            this.authorType = authorType;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }
    }
}
