package org.maksemses;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

public class TTSConverter {
    private final Voice voice;

    public TTSConverter(String voiceName) {
        Optional<Voice> voiceOptional = TTSVoice.provides()
                .stream()
                .filter(v -> voiceName.equals(v.getShortName()))
                .findFirst();
        if (!voiceOptional.isPresent()) {
            throw new IllegalStateException("Голос не найден: " + voiceName);
        }
        this.voice = voiceOptional.get();
    }

    /**
     * Преобразует текст в речь и возвращает путь к аудиофайлу.
     * @param content Текст для озвучивания.
     * @return Абсолютный путь к созданному .opus файлу.
     */
    public String textToSpeech(String content) {
        // Генерируем уникальное имя файла, чтобы избежать конфликтов
        String uniqueFileName = UUID.randomUUID().toString();

        TTS tts = new TTS(voice, content)
                .findHeadHook()
                .isRateLimited(true)
                .fileName(uniqueFileName)
                .overwrite(true)
                .formatMp3();

        tts.trans();

        // Библиотека сохраняет файлы в папку 'storage' относительно места запуска
        File audioFile = new File("storage/" + uniqueFileName + ".mp3");
        return audioFile.getAbsolutePath();
    }
}