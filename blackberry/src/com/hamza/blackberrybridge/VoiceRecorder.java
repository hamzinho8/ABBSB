package com.hamza.blackberrybridge;

public class VoiceRecorder {
    private boolean isRecording = false;

    public void startRecording() throws Exception {
        isRecording = true;
    }

    public byte[] stopRecording() {
        isRecording = false;
        return new byte[0];
    }
    
    public boolean isRecording() {
        return isRecording;
    }
}
