package com.marcosfgdev.mtickets.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SessionManager {

    private final Map<String, SessionEntry> sessions;
    private final Map<String, Long> oauthStates;
    private final SecureRandom random;
    private final long sessionTtlMillis;
    private final long stateTtlMillis;

    public SessionManager(long sessionTtlSeconds, long stateTtlSeconds) {
        this.sessions = new ConcurrentHashMap<>();
        this.oauthStates = new ConcurrentHashMap<>();
        this.random = new SecureRandom();
        this.sessionTtlMillis = TimeUnit.SECONDS.toMillis(Math.max(60L, sessionTtlSeconds));
        this.stateTtlMillis = TimeUnit.SECONDS.toMillis(Math.max(60L, stateTtlSeconds));
    }

    public String createSession(SessionUser user) {
        String token = generateToken();
        long expiresAt = System.currentTimeMillis() + sessionTtlMillis;
        user.setExpiresAt(expiresAt);
        sessions.put(token, new SessionEntry(user, expiresAt));
        return token;
    }

    public SessionUser getSession(String token) {
        if (token == null)
            return null;

        SessionEntry entry = sessions.get(token);
        if (entry == null)
            return null;

        if (entry.isExpired()) {
            sessions.remove(token);
            return null;
        }

        return entry.user;
    }

    public void invalidateSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void cleanExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        oauthStates.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
    }

    public String createOAuthState() {
        String state = generateToken();
        oauthStates.put(state, System.currentTimeMillis() + stateTtlMillis);
        return state;
    }

    public boolean consumeOAuthState(String state) {
        if (state == null || state.isEmpty()) {
            return false;
        }

        Long expiresAt = oauthStates.remove(state);
        return expiresAt != null && expiresAt >= System.currentTimeMillis();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static class SessionEntry {
        private final SessionUser user;
        private final long expiresAt;

        private SessionEntry(SessionUser user, long expiresAt) {
            this.user = user;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
