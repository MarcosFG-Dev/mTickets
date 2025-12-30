package com.marcosfgdev.mtickets.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<String, SessionUser> sessions;
    private final SecureRandom random;

    public SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.random = new SecureRandom();
    }

    public String createSession(SessionUser user) {
        String token = generateToken();
        sessions.put(token, user);
        System.out.println("[mTickets Session] Criada sessao para " + user.getUsername() + " (Token: "
                + token.substring(0, 10) + "...)");
        return token;
    }

    public SessionUser getSession(String token) {
        if (token == null)
            return null;

        SessionUser user = sessions.get(token);
        if (user == null)
            return null;

        if (user.isExpired()) {
            sessions.remove(token);
            return null;
        }

        return user;
    }

    public void invalidateSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void cleanExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
