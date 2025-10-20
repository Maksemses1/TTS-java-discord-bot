package org.maksemses.LavaPlayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            player.startTrack(queue.poll(), false);
        }

        String identifier = track.getIdentifier();

        File audioFile = new File(identifier);
        File audioFile2 = new File(identifier + ".vtt");

        if (audioFile.exists() && audioFile.getParentFile().getName().equals("storage")) {
            if (audioFile.delete() && audioFile2.delete()) {
                System.out.println("Временный аудиофайл успешно удален: " + identifier);
            } else {
                System.err.println("Не удалось удалить временный аудиофайл: " + identifier);
            }
        }
    }
}