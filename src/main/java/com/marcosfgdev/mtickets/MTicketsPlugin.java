package com.marcosfgdev.mtickets;

import com.marcosfgdev.mtickets.command.TicketCommand;
import com.marcosfgdev.mtickets.discord.DiscordBotService;
import com.marcosfgdev.mtickets.discord.DiscordOAuthService;
import com.marcosfgdev.mtickets.listener.MenuListener;
import com.marcosfgdev.mtickets.listener.TicketChatListener;
import com.marcosfgdev.mtickets.listener.TicketListListener;
import com.marcosfgdev.mtickets.security.SessionManager;
import com.marcosfgdev.mtickets.storage.Database;
import com.marcosfgdev.mtickets.storage.MySQLDatabase;
import com.marcosfgdev.mtickets.storage.SQLiteDatabase;
import com.marcosfgdev.mtickets.ticket.TicketManager;
import com.marcosfgdev.mtickets.web.WebServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class MTicketsPlugin extends JavaPlugin {

    private static MTicketsPlugin instance;

    private Database database;
    private TicketManager ticketManager;
    private SessionManager sessionManager;
    private DiscordOAuthService discordService;
    private DiscordBotService discordBotService;
    private WebServer webServer;
    private TicketListListener ticketListListener;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        extractWebResources();

        if (!initDatabase()) {
            getLogger().severe("Falha ao inicializar banco de dados! Desabilitando plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        ticketManager = new TicketManager(this, database);
        sessionManager = new SessionManager();

        if (getConfig().getBoolean("discord.enabled", true)) {
            discordService = new DiscordOAuthService(this);
            discordBotService = new DiscordBotService(this);
            new Thread(() -> discordBotService.start()).start();
        }

        ticketListListener = new TicketListListener(this);

        getCommand("ticket").setExecutor(new TicketCommand(this));

        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TicketChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(ticketListListener, this);

        if (getConfig().getBoolean("web.enabled", true)) {
            try {
                int port = getConfig().getInt("web.port", 8080);
                webServer = new WebServer(this, port);
                webServer.start();
                getLogger().info("Painel Web iniciado na porta " + port);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Erro ao iniciar servidor web", e);
            }
        }

        log("&a&l[mTickets] &aPlugin ativado com sucesso!");
        log("&7Versao: &f" + getDescription().getVersion());
        if (webServer != null) {
            log("&7Painel Web: &f" + getConfig().getString("web.base-url"));
        }
    }

    @Override
    public void onDisable() {
        if (discordBotService != null) {
            discordBotService.shutdown();
        }

        if (webServer != null) {
            try {
                webServer.stop();
                getLogger().info("Servidor web parado.");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Erro ao parar servidor web", e);
            }
        }

        if (database != null) {
            database.close();
        }

        log("&c&l[mTickets] &cPlugin desativado.");
        instance = null;
    }

    private boolean initDatabase() {
        String type = getConfig().getString("database.type", "sqlite").toLowerCase();

        try {
            if (type.equals("mysql")) {
                String host = getConfig().getString("database.mysql.host", "localhost");
                int port = getConfig().getInt("database.mysql.port", 3306);
                String dbName = getConfig().getString("database.mysql.database", "mtickets");
                String user = getConfig().getString("database.mysql.username", "root");
                String pass = getConfig().getString("database.mysql.password", "");

                database = new MySQLDatabase(host, port, dbName, user, pass);
                getLogger().info("Usando MySQL como banco de dados.");
            } else {
                String fileName = getConfig().getString("database.sqlite.file", "tickets.db");
                File dbFile = new File(getDataFolder(), fileName);
                database = new SQLiteDatabase(dbFile);
                getLogger().info("Usando SQLite como banco de dados.");
            }

            database.initialize();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro ao conectar ao banco de dados", e);
            return false;
        }
    }

    private void extractWebResources() {
        File webDir = new File(getDataFolder(), "web");
        if (!webDir.exists()) {
            webDir.mkdirs();
            saveResource("web/login.html", false);
            saveResource("web/panel.html", false);
            saveResource("web/app.js", false);
            saveResource("web/style.css", false);
        }
    }

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize(message));
    }

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getMessage(String path) {
        String prefix = getConfig().getString("messages.prefix", "&8[&bmTickets&8] &7");
        String message = getConfig().getString("messages." + path, "&cMensagem nao encontrada: " + path);
        return colorize(prefix + message);
    }

    public String getRawMessage(String path) {
        return colorize(getConfig().getString("messages." + path, ""));
    }

    public static MTicketsPlugin getInstance() {
        return instance;
    }

    public Database getTicketDatabase() {
        return database;
    }

    public TicketManager getTicketManager() {
        return ticketManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public DiscordOAuthService getDiscordService() {
        return discordService;
    }

    public DiscordBotService getDiscordBotService() {
        return discordBotService;
    }

    public WebServer getWebServer() {
        return webServer;
    }

    public TicketListListener getTicketListListener() {
        return ticketListListener;
    }
}
