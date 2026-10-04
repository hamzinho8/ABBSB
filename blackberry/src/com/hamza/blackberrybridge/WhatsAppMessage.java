package com.hamza.blackberrybridge;

/**
 * Modèle de message WhatsApp pour BlackBerry Curve 9300.
 */
public class WhatsAppMessage {
    public String notifId;
    public String senderName;
    public String body;
    public String timestamp;
    public boolean isOutgoing;
    public boolean isConfirmed;

    public WhatsAppMessage(String notifId, String senderName, String body, String timestamp, boolean isOutgoing) {
        this.notifId = notifId != null ? notifId : ("wa_" + System.currentTimeMillis());
        this.senderName = senderName != null ? senderName : "WhatsApp";
        this.body = body != null ? body : "";
        this.timestamp = timestamp != null ? timestamp : "";
        this.isOutgoing = isOutgoing;
        this.isConfirmed = !isOutgoing;
    }
}
