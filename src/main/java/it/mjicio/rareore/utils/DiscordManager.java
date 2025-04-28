package it.mjicio.rareore.utils;

import it.mjicio.rareore.AziendaPlugin;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.security.auth.login.LoginException;

public class DiscordManager {
    private final AziendaPlugin aziendaPlugin;

    public DiscordManager(AziendaPlugin aziendaPlugin) {
        this.aziendaPlugin = aziendaPlugin;
    }
    private net.dv8tion.jda.api.JDA jda;


    public void init() {
        try {
            String token = "bot-token";
            jda = JDABuilder.createDefault(token).build();
            jda.awaitReady(); // Bloch per assicurarsi che il bot sia pronto
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String channelId, String message) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            System.out.println("Channel not found");
        }
    }
}
