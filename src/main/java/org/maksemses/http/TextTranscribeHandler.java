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


public class TextTranscribeHandler implements HttpHandler {

    private final JDA jda;

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

            TextChannel channel = jda.getTextChannelById(targetChannelId);
            if (channel == null) {
                throw new IOException("Target text channel not found (ID: " + targetChannelId + ")");
            }

            InputStream is = exchange.getRequestBody();
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("[STT-HTTP-TEXT] Получен текст: " + text);

            if (text != null && !text.isBlank()) {

                final String filterString = "Редактор субтитров А.Синецкая Корректор А.Егорова";

                if (text.contains(filterString)) {
                    System.out.println("[STT-HTTP-TEXT] Сообщение отфильтровано (титры) и не будет отправлено.");
                } else {
                    channel.sendMessage(text).queue();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            responseMessage = e.getMessage();
            statusCode = 500;
        } finally {
            exchange.sendResponseHeaders(statusCode, responseMessage.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseMessage.getBytes());
            os.close();
        }
    }
}