package org.maksemses;

import com.sun.net.httpserver.HttpServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.maksemses.Listeners.MessageListener;
import org.maksemses.Listeners.SttTextListener;
import org.maksemses.http.TextTranscribeHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors; // <-- 1. ДОБАВЛЕН ИМПОРТ

public class TTSBot
{
    public static volatile Long sttTargetTextChannelId = null;

    public static void main(String[] args)
            throws InterruptedException, IOException
    {
        JDA jda = JDABuilder.createDefault(System.getenv("DISCORD_API_KEY"))
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES)
                .addEventListeners(
                        new MessageListener(),
                        new SttTextListener()
                )
                .build();

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 45678), 0);

            server.createContext("/transcribe", new TextTranscribeHandler(jda));

            // --- 2. ВОТ ИСПРАВЛЕНИЕ ---
            // Вместо server.setExecutor(null);
            // Даем серверу пул потоков для обработки запросов.
            // Теперь один "зависший" запрос не заблокирует весь сервер.
            server.setExecutor(Executors.newCachedThreadPool());
            // ------------------------

            server.start();
            System.out.println("HTTP-сервер запущен на порту 45678...");

        } catch (IOException e) {
            System.err.println("Не удалось запустить HTTP-сервер: " + e.getMessage());
            e.printStackTrace();
            jda.shutdown();
            return;
        }

        jda.awaitReady();
        System.out.println("Discord-бот готов!");
    }
}