package com.marcosfgdev.mtickets.discord;

import com.marcosfgdev.mtickets.MTicketsPlugin;
import com.marcosfgdev.mtickets.ticket.Ticket;
import com.marcosfgdev.mtickets.ticket.TicketManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class DiscordBotService extends ListenerAdapter {

    private final MTicketsPlugin plugin;
    private JDA jda;
    private String categoryId;
    private String guildId;

    public DiscordBotService(MTicketsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        String token = plugin.getConfig().getString("discord.bot-token");
        this.categoryId = plugin.getConfig().getString("discord.ticket-category-id");
        this.guildId = plugin.getConfig().getString("discord.guild-id");

        if (token == null || token.isEmpty() || "SEU_BOT_TOKEN_AQUI".equals(token)) {
            plugin.getLogger().warning("Bot Token nao configurado. Discord Bot desativado.");
            return;
        }

        try {
            JDABuilder builder = JDABuilder.createDefault(token);
            builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS);
            builder.addEventListeners(this);

            this.jda = builder.build();
            this.jda.awaitReady();

            plugin.getLogger().info("Discord Bot conectado como: " + jda.getSelfUser().getAsTag());

        } catch (LoginException e) {
            plugin.getLogger().severe("Erro de Login no Discord Bot: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("Inicializacao do Discord Bot interrompida.");
        }
    }

    public void startWithRetry() {
        int maxAttempts = plugin.getConfig().getInt("discord.connection.max-attempts", 5);
        long initialDelaySeconds = plugin.getConfig().getLong("discord.connection.initial-delay-seconds", 2L);
        long maxDelaySeconds = plugin.getConfig().getLong("discord.connection.max-delay-seconds", 30L);

        long delaySeconds = Math.max(1L, initialDelaySeconds);
        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            start();
            if (isEnabled()) {
                return;
            }

            if (attempt == maxAttempts) {
                plugin.getLogger().severe("Discord Bot nao conectou apos " + attempt + " tentativas.");
                return;
            }

            plugin.getLogger().warning("Tentativa " + attempt + " falhou. Nova tentativa em " + delaySeconds + "s.");
            try {
                TimeUnit.SECONDS.sleep(delaySeconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            delaySeconds = Math.min(delaySeconds * 2L, Math.max(1L, maxDelaySeconds));
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
    }

    public boolean isEnabled() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    public void createTicketChannel(Ticket ticket) {
        if (!isEnabled() || categoryId == null || categoryId.isEmpty())
            return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return;

        Category category = guild.getCategoryById(categoryId);
        if (category == null) {
            plugin.getLogger().warning("Categoria de Tickets do Discord nao encontrada: " + categoryId);
            return;
        }

        String channelName = "t-" + ticket.getId() + "-" + ticket.getPlayerName().toLowerCase();

        guild.createTextChannel(channelName, category)
                .setTopic("Ticket #" + ticket.getId() + " de " + ticket.getPlayerName())
                .queue(channel -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Ticket #" + ticket.getId());
                    embed.setColor(Color.CYAN);
                    embed.addField("Jogador", ticket.getPlayerName(), true);
                    embed.addField("Status", "Aberto", true);
                    embed.setDescription(ticket.getMessage());
                    embed.setFooter("mTickets System", null);
                    embed.setTimestamp(java.time.Instant.now());

                    channel.sendMessageEmbeds(embed.build()).queue();
                });
    }

    public void closeTicketChannel(Ticket ticket) {
        if (!isEnabled() || guildId == null)
            return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return;

        String channelName = "t-" + ticket.getId() + "-" + ticket.getPlayerName().toLowerCase();

        java.util.List<TextChannel> channels = guild.getTextChannelsByName(channelName, true);

        for (TextChannel channel : channels) {
            channel.delete().reason("Ticket Fechado").queue();
        }
    }

    public void sendTicketMessage(Ticket ticket, String author, String message) {
        if (!isEnabled())
            return;

        String channelName = "t-" + ticket.getId() + "-" + ticket.getPlayerName().toLowerCase();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return;

        java.util.List<TextChannel> channels = guild.getTextChannelsByName(channelName, true);
        if (channels.isEmpty())
            return;

        TextChannel channel = channels.get(0);

        String formatted = String.format("**%s**: %s", author, message);
        channel.sendMessage(formatted).queue();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;

        TextChannel channel = event.getChannel();
        String topic = channel.getTopic();

        if (topic != null && topic.startsWith("Ticket #")) {
            try {
                String idStr = topic.split(" ")[1].replace("#", "");
                int ticketId = Integer.parseInt(idStr);

                Ticket ticket = plugin.getTicketManager().getTicket(ticketId);
                if (ticket != null && (ticket.getStatus() == com.marcosfgdev.mtickets.ticket.TicketStatus.OPEN
                        || ticket.getStatus() == com.marcosfgdev.mtickets.ticket.TicketStatus.IN_PROGRESS)) {

                    String staffName = event.getMember() != null ? event.getMember().getEffectiveName()
                            : event.getAuthor().getName();
                    String message = event.getMessage().getContentDisplay();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getTicketManager().addReply(ticket, staffName, "STAFF", message, true);
                    });
                }

            } catch (Exception e) {
            }
        }
    }
}
