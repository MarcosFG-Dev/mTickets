package com.marcosfgdev.mtickets.security;

public class SessionUser {

    @com.google.gson.annotations.SerializedName("discordId")
    private String discordId;
    @com.google.gson.annotations.SerializedName("username")
    private String username;
    @com.google.gson.annotations.SerializedName("discriminator")
    private String discriminator;
    @com.google.gson.annotations.SerializedName("avatar")
    private String avatar;
    @com.google.gson.annotations.SerializedName("authorized")
    private boolean authorized;
    @com.google.gson.annotations.SerializedName("expiresAt")
    private long expiresAt;

    public SessionUser(String discordId, String username) {
        this.discordId = discordId;
        this.username = username;
        this.expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(String discriminator) {
        this.discriminator = discriminator;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public String getAvatarUrl() {
        if (avatar == null || avatar.isEmpty()) {
            return "https://cdn.discordapp.com/embed/avatars/0.png";
        }
        return "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatar + ".png";
    }
}
