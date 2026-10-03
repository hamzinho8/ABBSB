package com.hamza.blackberrybridge;

/**
 * Entité représentant un message SMS entrant ou sortant sur BlackBerry Curve 9300.
 */
public class SmsItem {
    public String id;
    public String senderNumber;
    public String senderName;
    public int slotSim; // 0 = SIM 1, 1 = SIM 2
    public String simName;
    public String timestamp;
    public String body;
    public boolean isOutgoing;
    public String status; // "Reçu", "Envoyé", "En attente...", "Échec"

    public SmsItem() {
        this.slotSim = 0;
        this.simName = "SIM 1";
        this.status = "Reçu";
    }

    public SmsItem(String id, String senderNumber, String senderName, int slotSim, String simName, String timestamp, String body, boolean isOutgoing) {
        this.id = id;
        this.senderNumber = senderNumber != null ? senderNumber : "";
        this.senderName = senderName != null && senderName.length() > 0 ? senderName : this.senderNumber;
        this.slotSim = slotSim;
        this.simName = simName != null && simName.length() > 0 ? simName : ("SIM " + (slotSim + 1));
        this.timestamp = timestamp != null ? timestamp : "";
        this.body = body != null ? body : "";
        this.isOutgoing = isOutgoing;
        this.status = isOutgoing ? "Envoyé" : "Reçu";
    }

    public String getDisplayName() {
        if (senderName != null && senderName.trim().length() > 0) {
            return senderName;
        }
        return senderNumber;
    }

    public String getSnippet(int maxLength) {
        if (body == null) return "";
        if (body.length() <= maxLength) return body;
        return body.substring(0, maxLength) + "...";
    }
}
