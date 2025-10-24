package org.maksemses.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.maksemses.TTSBot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * НОВЫЙ обработчик.
 * Принимает POST-запрос и отправляет полученный текст
 * в текстовый канал Discord, выбранный командой !stt-here.
 */
public class TextTranscribeHandler implements HttpHandler {

    private final JDA jda;

    // Этому обработчику нужен *только* JDA, чтобы отправлять сообщения
    public TextTranscribeHandler(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String responseMessage = "OK";
        int statusCode = 200;

        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                throw new IOException("Method Not Allowed");
            }

            // 1. Получаем ID целевого ТЕКСТОВОГО канала
            Long targetChannelId = TTSBot.sttTargetTextChannelId;
            if (targetChannelId == null) {
                throw new IOException("STT target channel not set. Use !stt-here in Discord first.");
            }

            // 2. Находим этот канал
            TextChannel channel = jda.getTextChannelById(targetChannelId);
            if (channel == null) {
                throw new IOException("Target text channel not found (ID: " + targetChannelId + ")");
            }

            // 3. (Проверки голоса НЕТ)
            // 4. Читаем текст из POST-запроса
            InputStream is = exchange.getRequestBody();
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("[STT-HTTP-TEXT] Получен текст: " + text);

            if (text != null && !text.isBlank()) {
                // 5. (Генерации речи НЕТ)
                // 6. (LavaPlayer НЕТ)

                // Просто отправляем текст в чат
                channel.sendMessage(text).queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
            responseMessage = e.getMessage();
            statusCode = 500;
        } finally {
            // 7. Отправляем ответ 200 (OK) обратно в VoiceRecorderApp
            exchange.sendResponseHeaders(statusCode, responseMessage.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseMessage.getBytes());
            os.close();
        }
    }
}