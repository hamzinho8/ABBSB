package com.hamza.blackberrybridge.audio;

import net.rim.device.api.media.control.AudioPathControl;

/**
 * Adaptateur de compatibilité audio pour BlackBerry Curve 9300.
 * Délégué direct vers le moteur temps réel {@link ZeroLagAudioPlayer}.
 */
public class BlackBerryAudioStreamer {
    private static BlackBerryAudioStreamer instance;

    public static synchronized BlackBerryAudioStreamer getInstance() {
        if (instance == null) {
            instance = new BlackBerryAudioStreamer();
        }
        return instance;
    }

    public void startAudio() {
        ZeroLagAudioPlayer.getInstance().start();
    }

    public void enqueueChunk(String base64Data) {
        ZeroLagAudioPlayer.getInstance().onAudioChunk(base64Data);
    }

    public void stopAudio() {
        ZeroLagAudioPlayer.getInstance().stop();
    }

    public boolean isRunning() {
        return ZeroLagAudioPlayer.getInstance().isRunning();
    }

    public int getChunksPlayed() {
        return ZeroLagAudioPlayer.getInstance().getChunksPlayed();
    }

    public void setVolume(int vol) {
        ZeroLagAudioPlayer.getInstance().setVolume(vol);
    }

    public int getVolume() {
        return ZeroLagAudioPlayer.getInstance().getVolume();
    }

    public void setForcedAudioPath(int path) {
        ZeroLagAudioPlayer.getInstance().setAudioRoute(path);
    }

    public void toggleSpeakerHandset() {
        ZeroLagAudioPlayer.getInstance().setSpeakerphone(!ZeroLagAudioPlayer.getInstance().isSpeakerphoneOn());
    }
}
