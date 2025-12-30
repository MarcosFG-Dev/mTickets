package com.marcosfgdev.mtickets.discord;

import com.google.gson.annotations.SerializedName;

public class DiscordUser {

    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("discriminator")
    private String discriminator;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("email")
    private String email;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullUsername() {
        if (discriminator != null && !discriminator.equals("0")) {
            return username + "#" + discriminator;
        }
        return username;
    }
}
