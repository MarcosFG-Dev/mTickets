package com.marcosfgdev.mtickets.ticket;

public enum TicketStatus {
    OPEN("Aberto", "&a"),
    IN_PROGRESS("Em Andamento", "&e"),
    WAITING_REPLY("Aguardando Resposta", "&6"),
    CLOSED("Fechado", "&c"),
    RESOLVED("Resolvido", "&2");

    private final String displayName;
    private final String colorCode;

    TicketStatus(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getColoredName() {
        return colorCode + displayName;
    }
}
