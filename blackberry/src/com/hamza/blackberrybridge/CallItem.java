package com.hamza.blackberrybridge;

/**
 * Modèle de données pour un appel de l'historique téléphonique.
 * Supporte les appels émis (sortants), reçus (entrants) et manqués,
 * avec la SIM utilisée, la date/heure et la durée de communication.
 */
public class CallItem {
    public static final int TYPE_INCOMING = 0;
    public static final int TYPE_OUTGOING = 1;
    public static final int TYPE_MISSED = 2;

    public String name;
    public String number;
    public String time;
    public int type;
    public String duration;
    public String simName;

    public CallItem(String name, String number, String time, int type, String duration, String simName) {
        this.name = (name != null && name.trim().length() > 0) ? name.trim() : number;
        this.number = (number != null) ? number.trim() : "";
        this.time = (time != null) ? time : "";
        this.type = type;
        this.duration = (duration != null) ? duration : "";
        this.simName = (simName != null && simName.trim().length() > 0) ? simName.trim() : "SIM 1";
    }

    public boolean isIncoming() {
        return type == TYPE_INCOMING;
    }

    public boolean isOutgoing() {
        return type == TYPE_OUTGOING;
    }

    public boolean isMissed() {
        return type == TYPE_MISSED;
    }
}
