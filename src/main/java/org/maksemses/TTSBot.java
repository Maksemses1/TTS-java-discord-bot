package org.maksemses;

import com.sun.net.httpserver.HttpServer; // Встроенный HTTP-сервер
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.maksemses.Listeners.MessageListener; // <-- ТВОЙ СТАРЫЙ ЛИСТЕНЕР
import org.maksemses.Listeners.SttTextListener; // <-- НАШ НОВЫЙ ЛИСТЕНЕР
import org.maksemses.http.TextTranscribeHandler; // <-- НАШ НОВЫЙ ОБРАБОТЧИК

import java.io.IOException;
import java.net.InetSocketAddress;

public class TTSBot
{
    // --- НОВОЕ ---
    // ID *текстового* канала, куда будет писать VoiceRecorderApp
    public static volatile Long sttTargetTextChannelId = null;
    // -------------

    public static void main(String[] args)
            throws InterruptedException, IOException // Добавлено IOException
    {
        // 1. Строим JDA
        JDA jda = JDABuilder.createDefault(System.getenv("DISCORD_API_KEY"))
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES)
                // --- РЕГИСТРИРУЕМ ОБА ЛИСТЕНЕРА ---
                .addEventListeners(
                        new MessageListener(),   // <-- Твой старый листенер (не трогаем)
                        new SttTextListener()  // <-- Наш новый листенер (для текста)
                )
                .build();

        // 2. Запускаем встроенный HTTP-сервер
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 45678), 0);

            // Используем новый TextTranscribeHandler
            // Ему нужен *только* 'jda', чтобы отправлять сообщения
            server.createContext("/transcribe", new TextTranscribeHandler(jda));

            server.setExecutor(null);
            server.start();
            System.out.println("HTTP-сервер запущен на порту 45678...");

        } catch (IOException e) {
            System.err.println("Не удалось запустить HTTP-сервер: " + e.getMessage());
            e.printStackTrace();
            jda.shutdown(); // Выключаем бота, если сервер не запустился
            return;
        }

        // 3. Ждем готовности JDA
        jda.awaitReady();
        System.out.println("Discord-бот готов!");
    }
}