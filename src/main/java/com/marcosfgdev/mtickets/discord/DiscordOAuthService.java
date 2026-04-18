package com.marcosfgdev.mtickets.discord;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.marcosfgdev.mtickets.MTicketsPlugin;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

public class DiscordOAuthService {

    private static final String DISCORD_API = "https://discord.com/api/v10";
    private static final String OAUTH_AUTHORIZE = "https://discord.com/api/oauth2/authorize";
    private static final String OAUTH_TOKEN = "https://discord.com/api/oauth2/token";

    private final MTicketsPlugin plugin;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private final String clientId;
    private final String clientSecret;
    private final String guildId;
    private final String botToken;
    private final List<String> allowedRoles;

    public DiscordOAuthService(MTicketsPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();

        this.clientId = plugin.getConfig().getString("discord.client-id", "");
        this.clientSecret = plugin.getConfig().getString("discord.client-secret", "");
        this.guildId = plugin.getConfig().getString("discord.guild-id", "");
        this.botToken = plugin.getConfig().getString("discord.bot-token", "");
        this.allowedRoles = plugin.getConfig().getStringList("discord.allowed-roles");
    }

    public String getAuthorizationUrl(String redirectUri, String state) {
        return OAUTH_AUTHORIZE + "?client_id=" + clientId
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&response_type=code"
                + "&scope=identify%20guilds.members.read"
                + "&state=" + urlEncode(state);
    }

    public String exchangeCodeForToken(String code, String redirectUri) throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .build();

        Request request = new Request.Builder()
                .url(OAUTH_TOKEN)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Erro ao trocar code: " + response.code());
            }

            JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
            return json.get("access_token").getAsString();
        }
    }

    public DiscordUser getUser(String accessToken) throws IOException {
        Request request = new Request.Builder()
                .url(DISCORD_API + "/users/@me")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Erro ao obter usuario: " + response.code());
            }

            return gson.fromJson(response.body().string(), DiscordUser.class);
        }
    }

    public boolean hasAllowedRole(String userId) {
        try {
            String url = DISCORD_API + "/guilds/" + guildId + "/members/" + userId;
            plugin.getLogger().info("API Call: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bot " + botToken)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    plugin.getLogger().warning("Erro ao verificar roles: " + response.code());
                    return false;
                }

                JsonObject member = gson.fromJson(response.body().string(), JsonObject.class);
                JsonArray roles = member.getAsJsonArray("roles");

                plugin.getLogger().info("Verificando roles para usuario " + userId);
                plugin.getLogger().info("Roles do usuario: " + roles.toString());
                plugin.getLogger().info("Roles permitidas: " + allowedRoles.toString());

                for (int i = 0; i < roles.size(); i++) {
                    String roleId = roles.get(i).getAsString();
                    if (allowedRoles.contains(roleId)) {
                        plugin.getLogger().info("Role permitida encontrada: " + roleId);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao verificar roles: " + e.getMessage());
        }

        return false;
    }

    public boolean isConfigured() {
        return !clientId.isEmpty() && !clientSecret.isEmpty()
                && !guildId.isEmpty() && !botToken.isEmpty();
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
