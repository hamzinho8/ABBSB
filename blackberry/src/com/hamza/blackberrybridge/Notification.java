package com.hamza.blackberrybridge;

public class Notification {
    public String id;
    public String app;
    public String sender;
    public String message;
    
    public Notification(String id, String app, String sender, String message) {
        this.id = id;
        this.app = app;
        this.sender = sender;
        this.message = message;
    }
    
    public String toString() {
        return app + " - " + sender;
    }
}
