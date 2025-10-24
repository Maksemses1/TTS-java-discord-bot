package org.maksemses.Listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.maksemses.TTSBot; // Импортируем главный класс

/**
 * НОВЫЙ листенер.
 * Отвечает ТОЛЬКО за выбор канала, куда VoiceRecorderApp будет
 * отправлять распознанный ТЕКСТ.
 * Не использует LavaPlayer и не подключается к голосу.
 */
public class SttTextListener extends ListenerAdapter {

    // (Конструктор не нужен, так как он не хранит состояние)

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        String content = event.getMessage().getContentRaw();
        String prefix = "!";

        if (content.equals(prefix + "stt-here")) {
            // 1. Запоминаем ID ТЕКСТОВОГО канала
            TTSBot.sttTargetTextChannelId = event.getChannel().getIdLong();

            event.getChannel().sendMessage(
                    "✅ **STT-Текст Активирован!**\n" +
                            "Я буду дублировать распознанный текст с `VoiceRecorderApp` сюда."
            ).queue();

        } else if (content.equals(prefix + "stt-stop")) {

            // 2. Проверяем, был ли этот канал целью
            if (TTSBot.sttTargetTextChannelId != null &&
                    TTSBot.sttTargetTextChannelId == event.getChannel().getIdLong()) {

                TTSBot.sttTargetTextChannelId = null;

                event.getChannel().sendMessage(
                        "✅ **STT-Текст Деактивирован.**"
                ).queue();
            }
        }
    }
}