package org.maksemses;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.maksemses.Listeners.MessageListener;

/*
public class ttsBot {
    public static void main(String[] args) {
        TTSConverter tts = new TTSConverter("ru-RU-DmitryNeural");
        tts.textToSpeach("Привет, Меня зовут Андрей");
    }
}*/
public class TTSBot
{
    public static void main(String[] args)
            throws InterruptedException
    {
        // Note: It is important to register your ReadyListener before building
        JDA jda = JDABuilder.createDefault(System.getenv("DISCORD_API_KEY"))
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES)
                .addEventListeners(new MessageListener())
                .build();

        // optionally block until JDA is ready
        jda.awaitReady();
    }
}