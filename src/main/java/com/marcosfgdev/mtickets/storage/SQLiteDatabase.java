package com.marcosfgdev.mtickets.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDatabase implements Database {

    private final File databaseFile;
    private final String connectionUrl;
    private TicketRepository ticketRepository;
    private final Object lock = new Object();

    public SQLiteDatabase(File databaseFile) {
        this.databaseFile = databaseFile;
        this.connectionUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
    }

    @Override
    public void initialize() throws Exception {

        if (!databaseFile.getParentFile().exists()) {
            databaseFile.getParentFile().mkdirs();
        }

        Class.forName("org.sqlite.JDBC");

        ticketRepository = new TicketRepository(this);
        ticketRepository.createTables();
    }

    @Override
    public Connection getConnection() throws SQLException {
        synchronized (lock) {
            return DriverManager.getConnection(connectionUrl);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public TicketRepository getTicketRepository() {
        return ticketRepository;
    }
}
