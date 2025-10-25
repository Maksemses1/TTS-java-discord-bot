package org.maksemses.Listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.maksemses.TTSBot; // Импортируем главный класс


public class SttTextListener extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        String content = event.getMessage().getContentRaw();
        String prefix = "!";

        if (content.equals(prefix + "stt-here")) {
            TTSBot.sttTargetTextChannelId = event.getChannel().getIdLong();

            event.getChannel().sendMessage(
                    "✅ **STT-Текст Активирован!**\n" +
                            "Я буду дублировать распознанный текст с `VoiceRecorderApp` сюда."
            ).queue();

        } else if (content.equals(prefix + "stt-stop")) {

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