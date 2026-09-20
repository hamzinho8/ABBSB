package com.hamza.blackberrybridge;

import net.rim.device.api.system.Application;

public class MediaManager {
    private SmartBridgeApp app;
    private String currentTitle = "Aucune Musique";
    private String currentArtist = "Artiste Inconnu";
    private String currentState = "STOPPED";
    private MediaScreen activeScreen;
    
    public MediaManager(SmartBridgeApp app) {
        this.app = app;
    }
    
    public void play() { app.getConnectionManager().sendData("MEDIA_PLAY\n"); }
    public void pause() { app.getConnectionManager().sendData("MEDIA_PAUSE\n"); }
    public void next() { app.getConnectionManager().sendData("MEDIA_NEXT\n"); }
    public void previous() { app.getConnectionManager().sendData("MEDIA_PREVIOUS\n"); }
    
    public void updateMedia(String title, String artist, String state) {
        this.currentTitle = title;
        this.currentArtist = artist;
        this.currentState = state;
        
        if (activeScreen != null) {
            Application.getApplication().invokeLater(new Runnable() {
                public void run() {
                    activeScreen.refreshMedia();
                }
            });
        }
    }
    
    public String getTitle() { return currentTitle; }
    public String getArtist() { return currentArtist; }
    public String getState() { return currentState; }
    
    public void setActiveScreen(MediaScreen screen) {
        this.activeScreen = screen;
    }
}
