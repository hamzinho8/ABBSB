package com.hamza.blackberrybridge;

import net.rim.device.api.ui.UiApplication;

public class MediaManager {
    private SmartBridgeApp app;
    private String currentTitle = "Starboy (ft. Daft Punk)";
    private String currentArtist = "The Weeknd";
    private String currentState = "PLAYING";
    private MediaScreen activeScreen;
    
    public MediaManager(SmartBridgeApp app) {
        this.app = app;
    }
    
    public void play() {
        this.currentState = "PLAYING";
        app.getConnectionManager().sendData("MEDIA_PLAY\n");
        if (activeScreen != null) activeScreen.refreshMedia();
    }

    public void pause() {
        this.currentState = "PAUSED";
        app.getConnectionManager().sendData("MEDIA_PAUSE\n");
        if (activeScreen != null) activeScreen.refreshMedia();
    }

    public void next() {
        app.getConnectionManager().sendData("MEDIA_NEXT\n");
    }

    public void previous() {
        app.getConnectionManager().sendData("MEDIA_PREVIOUS\n");
    }
    
    public void updateMedia(String title, String artist, String state) {
        this.currentTitle = (title != null && title.length() > 0) ? title : currentTitle;
        this.currentArtist = (artist != null && artist.length() > 0) ? artist : currentArtist;
        this.currentState = (state != null && state.length() > 0) ? state : currentState;
        
        if (activeScreen != null) {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    if (activeScreen != null) activeScreen.refreshMedia();
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
