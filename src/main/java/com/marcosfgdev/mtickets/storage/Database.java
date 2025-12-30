package com.marcosfgdev.mtickets.storage;

import java.sql.Connection;

public interface Database {

    void initialize() throws Exception;

    Connection getConnection() throws Exception;

    void close();

    TicketRepository getTicketRepository();
}
