package org.maksemses.Listeners;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.maksemses.LavaPlayer.GuildMusicManager; // Используем ваш пакет
import org.maksemses.TTSConverter;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageListener extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final TTSConverter ttsConverter;

    // --- НОВОЕ: Хранилище для отслеживаемых каналов ---
    // Ключ: ID сервера (Guild), Значение: ID текстового канала для озвучки
    private final Map<Long, Long> monitoredChannels;

    public MessageListener() {
        // Используем ConcurrentHashMap для потокобезопасности
        this.musicManagers = new ConcurrentHashMap<>();
        this.monitoredChannels = new ConcurrentHashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        this.ttsConverter = new TTSConverter("ru-RU-DmitryNeural");

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        // Используем computeIfAbsent для более лаконичной инициализации
        return musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            GuildMusicManager newManager = new GuildMusicManager(playerManager);
            guild.getAudioManager().setSendingHandler(newManager.getSendHandler());
            return newManager;
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        String content = event.getMessage().getContentRaw();
        String prefix = "!";

        // --- 1. ПРОВЕРЯЕМ, ЯВЛЯЕТСЯ ЛИ СООБЩЕНИЕ КОМАНДОЙ ---
        if (content.startsWith(prefix)) {
            handleCommand(event, content, prefix);
            return; // Завершаем, так как команда была обработана
        }

        // --- 2. ЕСЛИ ЭТО НЕ КОМАНДА, ПРОВЕРЯЕМ, НУЖНО ЛИ ЕГО ОЗВУЧИВАТЬ ---
        Long monitoredChannelId = monitoredChannels.get(event.getGuild().getIdLong());
        if (monitoredChannelId != null && event.getChannel().getIdLong() == monitoredChannelId) {
            speakMessage(event, content);
        }
    }

    /**
     * Обрабатывает команды, такие как !listen и !unlisten.
     */
    private void handleCommand(MessageReceivedEvent event, String content, String prefix) {
        if (content.startsWith(prefix + "listen")) {
            if (event.getMessage().getMentions().getChannels().isEmpty()) {
                event.getChannel().sendMessage("❌ Укажите текстовый канал, который нужно озвучивать, например: `!listen #general`").queue();
                return;
            }
            TextChannel targetChannel = (TextChannel) event.getMessage().getMentions().getChannels().get(0);
            monitoredChannels.put(event.getGuild().getIdLong(), targetChannel.getIdLong());
            connectToVoiceChannel(event); // Подключаемся к голосовому каналу
            event.getChannel().sendMessage("✅ Начинаю озвучивать сообщения в канале " + targetChannel.getAsMention()).queue();

        } else if (content.equals(prefix + "unlisten")) {
            if (monitoredChannels.remove(event.getGuild().getIdLong()) != null) {
                event.getGuild().getAudioManager().closeAudioConnection(); // Отключаемся от голоса
                event.getChannel().sendMessage("✅ Озвучивание остановлено.").queue();
            } else {
                event.getChannel().sendMessage("❌ Бот и так не озвучивал никакой канал.").queue();
            }
        }
    }

    /**
     * Генерирует и воспроизводит речь из текста сообщения.
     */
    private void speakMessage(MessageReceivedEvent event, String text) {
        // Проверяем, на месте ли бот
        if (!event.getGuild().getAudioManager().isConnected()) {
            monitoredChannels.remove(event.getGuild().getIdLong());
            event.getChannel().sendMessage("ℹ️ Я был отключен от голосового канала, озвучивание остановлено. Используйте `!listen`, чтобы начать заново.").queue();
            return;
        }
        String filePath = ttsConverter.textToSpeech(text);
        // Вызываем loadAndPlay без отправки сообщения в чат
        loadAndPlay(event, filePath, false);
    }

    /**
     * Загружает трек и добавляет в очередь.
     * @param sendMessage true, если нужно отправить подтверждение в чат
     */
    private void loadAndPlay(final MessageReceivedEvent event, final String trackIdentifier, final boolean sendMessage) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());
        playerManager.loadItemOrdered(musicManager, trackIdentifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                connectToVoiceChannel(event);
                musicManager.scheduler.queue(track);
                if (sendMessage) {
                    // Определяем, это файл или ссылка, для красивого вывода
                    String title = track.getIdentifier().contains(File.separator) ? "Сгенерированная речь" : track.getInfo().title;
                    event.getChannel().sendMessage("✅ Добавляю в очередь: " + title).queue();
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                event.getChannel().sendMessage("❌ Плейлисты пока не поддерживаются.").queue();
            }

            @Override
            public void noMatches() {
                if (sendMessage) event.getChannel().sendMessage("❌ Ничего не найдено по запросу: " + trackIdentifier).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if (sendMessage) event.getChannel().sendMessage("❌ Не удалось загрузить трек: " + exception.getMessage()).queue();
            }
        });
    }

    private void connectToVoiceChannel(MessageReceivedEvent event) {
        AudioManager audioManager = event.getGuild().getAudioManager();
        if (audioManager.isConnected()) return;

        if (event.getMember() == null || event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
            event.getChannel().sendMessage("❌ Вы должны находиться в голосовом канале, чтобы я мог подключиться!").queue();
            return;
        }

        AudioChannel voiceChannel = event.getMember().getVoiceState().getChannel();
        audioManager.openAudioConnection(voiceChannel);
    }
}