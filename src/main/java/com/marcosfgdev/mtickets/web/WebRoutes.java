package com.marcosfgdev.mtickets.web;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.marcosfgdev.mtickets.discord.DiscordOAuthService;
import com.marcosfgdev.mtickets.discord.DiscordUser;
import com.marcosfgdev.mtickets.security.RateLimiter;
import com.marcosfgdev.mtickets.security.SessionUser;
import com.marcosfgdev.mtickets.ticket.Ticket;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebRoutes implements HttpHandler {

    private final MTicketsPlugin plugin;
    private final RateLimiter rateLimiter;

    public WebRoutes(MTicketsPlugin plugin) {
        this.plugin = plugin;
        this.rateLimiter = new RateLimiter(300, 60000);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (path.startsWith("/api")) {
            path = path.substring(4);
        }
        if (path.isEmpty())
            path = "/";

        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!ip.equals("127.0.0.1") && !ip.equals("0:0:0:0:0:0:0:1") && !rateLimiter.isAllowed(ip)) {
            sendError(exchange, 429, "Rate limit exceeded");
            return;
        }

        if (!applyCors(exchange)) {
            return;
        }

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if ("GET".equals(method)) {
                handleGet(exchange, path);
            } else if ("POST".equals(method)) {
                handlePost(exchange, path);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro na rota " + path + ": " + e.getMessage());
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        switch (path) {
            case "/auth/discord":
                handleDiscordAuth(exchange);
                break;
            case "/auth/callback":
                handleDiscordCallback(exchange);
                break;
            case "/auth/me":
                handleGetMe(exchange);
                break;
            case "/auth/logout":
                handleLogout(exchange);
                break;
            case "/tickets":
                handleGetTickets(exchange);
                break;
            case "/stats":
                handleGetStats(exchange);
                break;
            default:
                if (path.matches("/tickets/\\d+")) {
                    handleGetTicket(exchange, path);
                } else {
                    sendError(exchange, 404, "Not found");
                }
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (path.matches("/tickets/\\d+/reply")) {
            handleReply(exchange, path);
        } else if (path.matches("/tickets/\\d+/close")) {
            handleClose(exchange, path);
        } else {
            sendError(exchange, 404, "Not found");
        }
    }

    private void handleDiscordAuth(HttpExchange exchange) throws IOException {
        DiscordOAuthService discord = plugin.getDiscordService();
        if (discord == null || !discord.isConfigured()) {
            sendError(exchange, 500, "Discord nao configurado");
            return;
        }

        String baseUrl = plugin.getConfig().getString("web.base-url");
        String redirectUri = baseUrl + "/api/auth/callback";
        String state = plugin.getSessionManager().createOAuthState();
        String authUrl = discord.getAuthorizationUrl(redirectUri, state);

        redirect(exchange, authUrl);
    }

    private void handleDiscordCallback(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);
        String code = params.get("code");
        String state = params.get("state");

        if (code == null || code.isEmpty() || !plugin.getSessionManager().consumeOAuthState(state)) {
            redirect(exchange, "/login.html?error=no_code");
            return;
        }

        DiscordOAuthService discord = plugin.getDiscordService();
        String baseUrl = plugin.getConfig().getString("web.base-url");
        String redirectUri = baseUrl + "/api/auth/callback";

        try {
            String accessToken = discord.exchangeCodeForToken(code, redirectUri);
            DiscordUser user = discord.getUser(accessToken);

            if (!discord.hasAllowedRole(user.getId())) {
                redirect(exchange, "/login.html?error=no_permission");
                return;
            }

            SessionUser sessionUser = new SessionUser(user.getId(), user.getUsername());
            sessionUser.setDiscriminator(user.getDiscriminator());
            sessionUser.setAvatar(user.getAvatar());
            sessionUser.setAuthorized(true);

            String token = plugin.getSessionManager().createSession(sessionUser);

            Headers headers = exchange.getResponseHeaders();
            headers.add("Set-Cookie", buildSessionCookie(token));

            redirect(exchange, "/panel.html");

        } catch (Exception e) {
            plugin.getLogger().warning("Erro no callback OAuth: " + e.getMessage());
            redirect(exchange, "/login.html?error=auth_failed");
        }
    }

    private void handleGetMe(HttpExchange exchange) throws IOException {
        SessionUser user = getSessionUser(exchange);
        if (user == null) {
            sendError(exchange, 401, "Nao autenticado");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getDiscordId());
        data.put("username", user.getUsername());
        data.put("avatar", user.getAvatarUrl());

        sendJson(exchange, data);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = getSessionToken(exchange);
        if (token != null) {
            plugin.getSessionManager().invalidateSession(token);
        }

        Headers headers = exchange.getResponseHeaders();
        headers.add("Set-Cookie", buildSessionCookie("", 0));

        redirect(exchange, "/login.html");
    }

    private void handleGetTickets(HttpExchange exchange) throws IOException {
        SessionUser user = getSessionUser(exchange);
        if (user == null) {
            sendError(exchange, 401, "Nao autenticado");
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int page = parseInt(params.get("page"), 1);
        int perPage = parseInt(params.get("perPage"), 20);

        List<Ticket> tickets = plugin.getTicketManager().getAllTickets(page, perPage);
        int total = plugin.getTicketManager().getTotalTicketCount();

        Map<String, Object> data = new HashMap<>();
        data.put("tickets", tickets);
        data.put("total", total);
        data.put("page", page);
        data.put("perPage", perPage);

        sendJson(exchange, data);
    }

    private void handleGetTicket(HttpExchange exchange, String path) throws IOException {
        SessionUser user = getSessionUser(exchange);
        if (user == null) {
            sendError(exchange, 401, "Nao autenticado");
            return;
        }

        try {
            int id = Integer.parseInt(path.replace("/tickets/", ""));
            Ticket ticket = plugin.getTicketManager().getTicket(id);

            if (ticket == null) {
                sendError(exchange, 404, "Ticket nao encontrado");
                return;
            }

            sendJson(exchange, ticket);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "ID invalido");
        }
    }

    private void handleReply(HttpExchange exchange, String path) throws IOException {
        SessionUser user = getSessionUser(exchange);
        if (user == null) {
            sendError(exchange, 401, "Nao autenticado");
            return;
        }

        try {
            String idStr = path.replaceAll("/tickets/(\\d+)/reply", "$1");
            int id = Integer.parseInt(idStr);

            String body = readBody(exchange);
            Map<String, String> data = JsonUtil.fromJson(body, Map.class);
            String message = data.get("message");

            if (message == null || message.trim().isEmpty()) {
                sendError(exchange, 400, "Mensagem vazia");
                return;
            }

            plugin.getTicketManager().addReply(id, user.getUsername(), "STAFF", message);
            sendJson(exchange, "{\"success\": true}");

        } catch (Exception e) {
            sendError(exchange, 500, "Erro: " + e.getMessage());
        }
    }

    private void handleClose(HttpExchange exchange, String path) throws IOException {
        SessionUser user = getSessionUser(exchange);
        if (user == null) {
            sendError(exchange, 401, "Nao autenticado");
            return;
        }

        try {
            String idStr = path.replaceAll("/tickets/(\\d+)/close", "$1");
            int id = Integer.parseInt(idStr);

            plugin.getTicketManager().closeTicket(id, user.getUsername());
            sendJson(exchange, "{\"success\": true}");

        } catch (Exception e) {
            sendError(exchange, 500, "Erro: " + e.getMessage());
        }
    }

    private SessionUser getSessionUser(HttpExchange exchange) {
        String token = getSessionToken(exchange);
        if (token == null)
            return null;
        return plugin.getSessionManager().getSession(token);
    }

    private String getSessionToken(HttpExchange exchange) {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies == null)
            return null;

        for (String cookie : cookies.split(";")) {
            cookie = cookie.trim();
            if (cookie.startsWith("session=")) {
                return cookie.substring(8);
            }
        }
        return null;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty())
            return params;

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                try {
                    params.put(pair[0], URLDecoder.decode(pair[1], "UTF-8"));
                } catch (Exception e) {
                    params.put(pair[0], pair[1]);
                }
            }
        }
        return params;
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void sendJson(HttpExchange exchange, Object data) throws IOException {
        String json = JsonUtil.toJson(data);
        sendJson(exchange, json);
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = JsonUtil.error(message);
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    private boolean applyCors(HttpExchange exchange) throws IOException {
        Headers requestHeaders = exchange.getRequestHeaders();
        Headers responseHeaders = exchange.getResponseHeaders();
        String origin = requestHeaders.getFirst("Origin");
        String allowedOrigin = getAllowedOrigin();

        responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        responseHeaders.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        responseHeaders.set("Access-Control-Allow-Credentials", "true");

        if (origin == null || origin.isEmpty()) {
            responseHeaders.set("Access-Control-Allow-Origin", allowedOrigin);
            return true;
        }

        if (!origin.equalsIgnoreCase(allowedOrigin)) {
            sendError(exchange, 403, "Origin nao permitida");
            return false;
        }

        responseHeaders.set("Access-Control-Allow-Origin", allowedOrigin);
        return true;
    }

    private String getAllowedOrigin() {
        try {
            String configured = plugin.getConfig().getString("web.base-url", "http://localhost:8080");
            URI uri = URI.create(configured);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "http://localhost:8080";
            }

            int port = uri.getPort();
            String hostPort = port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
            return uri.getScheme() + "://" + hostPort;
        } catch (Exception e) {
            return "http://localhost:8080";
        }
    }

    private String buildSessionCookie(String token) {
        int maxAge = plugin.getConfig().getInt("web.session.ttl-seconds", 86400);
        return buildSessionCookie(token, maxAge);
    }

    private String buildSessionCookie(String token, int maxAge) {
        String sameSite = plugin.getConfig().getString("web.session.same-site", "Lax");
        boolean secure = plugin.getConfig().getBoolean("web.session.secure-cookie", false);

        StringBuilder cookie = new StringBuilder();
        cookie.append("session=").append(token)
                .append("; Path=/")
                .append("; Max-Age=").append(Math.max(0, maxAge))
                .append("; HttpOnly")
                .append("; SameSite=").append(("Strict".equalsIgnoreCase(sameSite) ? "Strict" : "Lax"));

        if (secure) {
            cookie.append("; Secure");
        }

        return cookie.toString();
    }

    private void handleGetStats(HttpExchange exchange) throws IOException {
        SessionUser user = getSessionUser(exchange);
        if (user == null) {
            sendError(exchange, 401, "Nao autenticado");
            return;
        }

        int openCount = 0;
        int resolvedCount = 0;
        long avgTime = 0;

        try (java.sql.Connection conn = plugin.getTicketDatabase().getConnection();
                java.sql.Statement stmt = conn.createStatement()) {

            try (java.sql.ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM tickets WHERE status IN ('OPEN', 'IN_PROGRESS', 'WAITING_REPLY')")) {
                if (rs.next())
                    openCount = rs.getInt(1);
            }

            try (java.sql.ResultSet rs = stmt
                    .executeQuery("SELECT COUNT(*) FROM tickets WHERE status IN ('CLOSED', 'RESOLVED')")) {
                if (rs.next())
                    resolvedCount = rs.getInt(1);
            }

            try (java.sql.ResultSet rs = stmt.executeQuery(
                    "SELECT AVG(updated_at - created_at) FROM tickets WHERE status IN ('CLOSED', 'RESOLVED')")) {
                if (rs.next())
                    avgTime = rs.getLong(1);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao calcular stats: " + e.getMessage());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("openTickets", openCount);
        stats.put("resolvedTickets", resolvedCount);
        stats.put("avgTime", avgTime);

        long minutes = (avgTime / 1000) / 60;
        if (minutes < 60) {
            stats.put("avgTimeFormatted", minutes + " min");
        } else {
            long hours = minutes / 60;
            stats.put("avgTimeFormatted", hours + " h");
        }

        sendJson(exchange, stats);
    }
}
