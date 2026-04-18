package com.marcosfgdev.mtickets.web;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.Executors;

public class WebServer {

    private final MTicketsPlugin plugin;
    private final int port;
    private HttpServer server;

    public WebServer(MTicketsPlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void start() throws Exception {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (java.net.BindException e) {
            plugin.getLogger().severe("================================================");
            plugin.getLogger().severe("ERRO CRITICO: A porta " + port + " ja esta em uso!");
            plugin.getLogger().severe("Isso acontece se voce usou /reload ou outro plugin usa a mesma porta.");
            plugin.getLogger().severe("Mude a porta no config.yml ou reinicie o servidor completamente.");
            plugin.getLogger().severe("================================================");
            throw e;
        }

        WebRoutes routes = new WebRoutes(plugin);
        server.createContext("/api/", routes);

        server.createContext("/", new StaticFileHandler(plugin));

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        plugin.getLogger().info("Servidor web iniciado na porta " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    private static class StaticFileHandler implements HttpHandler {
        private final MTicketsPlugin plugin;
        private final File webDir;

        public StaticFileHandler(MTicketsPlugin plugin) {
            this.plugin = plugin;
            this.webDir = new File(plugin.getDataFolder(), "web");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/") || path.isEmpty()) {
                path = "/login.html";
            }

            File file = new File(webDir, path);

            if (!file.exists() || !file.getCanonicalPath().startsWith(webDir.getCanonicalPath())) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            String contentType = getContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody();
                    FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html"))
                return "text/html; charset=UTF-8";
            if (path.endsWith(".css"))
                return "text/css; charset=UTF-8";
            if (path.endsWith(".js"))
                return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json"))
                return "application/json; charset=UTF-8";
            if (path.endsWith(".png"))
                return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
                return "image/jpeg";
            if (path.endsWith(".gif"))
                return "image/gif";
            if (path.endsWith(".svg"))
                return "image/svg+xml";
            if (path.endsWith(".ico"))
                return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
