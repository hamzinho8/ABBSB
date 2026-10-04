package com.hamza.blackberrybridge;

import java.util.Calendar;
import java.util.Vector;
import com.hamza.blackberrybridge.audio.FastBase64;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

/**
 * Gestionnaire dédié aux fonctionnalités WhatsApp pour BlackBerry Curve 9300.
 * Gère les appels vocaux WhatsApp (entrant, décrochage, rejet, raccrochage)
 * et la messagerie instantanée WhatsApp (réception, décodage UTF-8, réponses rapides).
 */
public class WhatsAppManager {
    private SmartBridgeApp app;
    private final Vector messages = new Vector();
    private final Vector listeners = new Vector();

    private String activeCallId;
    private String activeCallerName;
    private boolean isCallRinging = false;
    private boolean isCallConnected = false;
    private WhatsAppIncomingCallScreen activeCallScreen;

    public interface WhatsAppListener {
        void onMessagesUpdated();
        void onNewMessage(WhatsAppMessage msg);
        void onCallEnded(String callId);
    }

    public WhatsAppManager(SmartBridgeApp app) {
        this.app = app;
        seedInitialMessages();
    }

    private void seedInitialMessages() {
        messages.addElement(new WhatsAppMessage("wa_1", "Karim Bennani", "Salut Hamza, tu es disponible pour le point d'avancement ?", "15:20", false));
        messages.addElement(new WhatsAppMessage("wa_2", "Dr. Lahlou", "Les résultats sont prêts, rappelle-moi dès que tu peux.", "14:45", false));
        messages.addElement(new WhatsAppMessage("wa_3", "Amina M.", "Message reçu ! Je prépare le dossier.", "Hier", true));
        messages.addElement(new WhatsAppMessage("wa_4", "Groupe Dev BlackBerry", "Le build OS 6.0 pour Curve 9300 est validé !", "Hier", false));
    }

    public synchronized Vector getMessages() {
        Vector copy = new Vector(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            copy.addElement(messages.elementAt(i));
        }
        return copy;
    }

    public synchronized void addListener(WhatsAppListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.addElement(listener);
        }
    }

    public synchronized void removeListener(WhatsAppListener listener) {
        if (listener != null) {
            listeners.removeElement(listener);
        }
    }

    private void notifyUpdated() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                for (int i = 0; i < listeners.size(); i++) {
                    try {
                        ((WhatsAppListener) listeners.elementAt(i)).onMessagesUpdated();
                    } catch (Throwable ignored) {}
                }
            }
        });
    }

    // =========================================================================
    // Gestion des Appels WhatsApp
    // =========================================================================

    /**
     * Traite un appel WhatsApp entrant reçu d'Android :
     * "WHATSAPP_CALL_INCOMING|<callId>|<callerName>\n"
     */
    public void handleIncomingCall(final String callId, final String callerName) {
        this.activeCallId = callId;
        this.activeCallerName = (callerName != null && callerName.length() > 0) ? callerName : "Contact WhatsApp";
        this.isCallRinging = true;
        this.isCallConnected = false;

        LogManager.log("WHATSAPP", "Incoming WhatsApp call: " + callerName + " (" + callId + ")");

        // 1. Alerte matérielle : Vibration continue et LED verte clignotante
        HardwareManager.startWhatsAppCallAlert();

        // 2. Affichage plein écran 'WhatsAppIncomingCallScreen'
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (activeCallScreen != null) {
                        try { activeCallScreen.close(); } catch (Throwable ignored) {}
                        activeCallScreen = null;
                    }
                    activeCallScreen = new WhatsAppIncomingCallScreen(WhatsAppManager.this, callId, activeCallerName);
                    UiApplication.getUiApplication().pushScreen(activeCallScreen);
                } catch (Throwable t) {
                    System.out.println("[BB ERROR] " + t.getMessage());
                }
            }
        });
    }

    /**
     * Décroche l'appel WhatsApp (Touche Verte) :
     * Envoie "WHATSAPP_CALL_ANSWER|<callId>\n" vers Android
     */
    public void answerCall(final String callId) {
        isCallRinging = false;
        isCallConnected = true;

        HardwareManager.stopWhatsAppCallAlert();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String packet = "WHATSAPP_CALL_ANSWER|" + callId + "\n";
                    LogManager.log("WHATSAPP", "Answering WhatsApp call: " + packet.trim());
                    app.getConnectionManager().sendData(packet);
                } catch (Throwable t) {
                    LogManager.error("WHATSAPP", "Answer error: " + t.getMessage());
                }
            }
        }).start();
    }

    /**
     * Rejette ou raccroche l'appel WhatsApp (Touche Rouge) :
     * Envoie "WHATSAPP_CALL_REJECT|<callId>\n" vers Android
     */
    public void rejectCall(final String callId) {
        isCallRinging = false;
        isCallConnected = false;

        HardwareManager.stopWhatsAppCallAlert();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String packet = "WHATSAPP_CALL_REJECT|" + (callId != null ? callId : "") + "\n";
                    LogManager.log("WHATSAPP", "Rejecting WhatsApp call: " + packet.trim());
                    app.getConnectionManager().sendData(packet);
                } catch (Throwable t) {
                    LogManager.error("WHATSAPP", "Reject error: " + t.getMessage());
                }
            }
        }).start();

        handleCallEnded(callId);
    }

    /**
     * Fin d'appel WhatsApp signalée par Android :
     * "WHATSAPP_CALL_ENDED|<callId>\n"
     */
    public void handleCallEnded(final String callId) {
        isCallRinging = false;
        isCallConnected = false;
        HardwareManager.stopWhatsAppCallAlert();

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (activeCallScreen != null) {
                        activeCallScreen.onCallTerminated();
                        activeCallScreen = null;
                    }
                    for (int i = 0; i < listeners.size(); i++) {
                        try {
                            ((WhatsAppListener) listeners.elementAt(i)).onCallEnded(callId);
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable t) {
                    System.out.println("[BB ERROR] " + t.getMessage());
                }
            }
        });
    }

    public boolean isCallActive() {
        return isCallConnected || isCallRinging;
    }

    public String getActiveCallerName() {
        return activeCallerName;
    }

    // =========================================================================
    // Gestion des Messages WhatsApp
    // =========================================================================

    /**
     * Traite un message WhatsApp entrant reçu d'Android :
     * "WHATSAPP_MSG|<notifId>|<senderName>|<base64Body>|<timestamp>\n"
     */
    public void handleIncomingMessage(String notifId, String senderName, String base64Body, String timestamp) {
        String decodedText = FastBase64.decodeString(base64Body);
        if (decodedText == null || decodedText.length() == 0) {
            decodedText = (base64Body != null) ? base64Body : "";
        }

        String time = (timestamp != null && timestamp.length() > 0) ? timestamp : formatCurrentTime();
        final WhatsAppMessage msg = new WhatsAppMessage(notifId, senderName, decodedText, time, false);

        synchronized (this) {
            messages.addElement(msg);
            if (messages.size() > 60) {
                messages.removeElementAt(0);
            }
        }

        // Alerte : Bip doux, vibration courte et LED verte
        HardwareManager.triggerWhatsAppMessageAlert();

        notifyUpdated();

        // Affichage popup ou notification
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                for (int i = 0; i < listeners.size(); i++) {
                    try {
                        ((WhatsAppListener) listeners.elementAt(i)).onNewMessage(msg);
                    } catch (Throwable ignored) {}
                }
            }
        });
    }

    /**
     * Envoie une réponse rapide à un message WhatsApp :
     * "WHATSAPP_REPLY|<notifId>|<base64ReplyText>\n"
     */
    public void sendReply(final String notifId, final String text) {
        if (text == null || text.trim().length() == 0) return;

        final String cleanText = text.trim();
        final String b64 = FastBase64.encodeString(cleanText);

        final WhatsAppMessage replyMsg = new WhatsAppMessage(
            notifId,
            "Moi",
            cleanText,
            formatCurrentTime(),
            true
        );

        synchronized (this) {
            messages.addElement(replyMsg);
        }
        notifyUpdated();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String packet = "WHATSAPP_REPLY|" + notifId + "|" + b64 + "\n";
                    LogManager.log("WHATSAPP", "Sending reply packet: " + packet.trim());
                    app.getConnectionManager().sendData(packet);
                } catch (Throwable t) {
                    LogManager.error("WHATSAPP", "Reply send error: " + t.getMessage());
                }
            }
        }).start();
    }

    /**
     * Démarre un nouveau chat WhatsApp :
     * "WHATSAPP_START_CHAT|<numéroTéléphone>|<base64Message>\n"
     */
    public void startNewChat(final String phone, final String message) {
        if (phone == null || phone.trim().length() == 0) return;

        final String cleanPhone = phone.trim();
        final String b64 = FastBase64.encodeString(message != null ? message : "");

        new Thread(new Runnable() {
            public void run() {
                try {
                    String packet = "WHATSAPP_START_CHAT|" + cleanPhone + "|" + b64 + "\n";
                    LogManager.log("WHATSAPP", "Starting new WhatsApp chat: " + packet.trim());
                    app.getConnectionManager().sendData(packet);
                } catch (Throwable t) {
                    LogManager.error("WHATSAPP", "Start chat error: " + t.getMessage());
                }
            }
        }).start();
    }

    /**
     * Confirmation d'envoi reçue d'Android :
     * "WHATSAPP_REPLY_OK|<notifId>\n"
     */
    public void handleReplyOk(final String notifId) {
        synchronized (this) {
            for (int i = 0; i < messages.size(); i++) {
                WhatsAppMessage m = (WhatsAppMessage) messages.elementAt(i);
                if (m.isOutgoing && (notifId == null || notifId.length() == 0 || m.notifId.equals(notifId))) {
                    m.isConfirmed = true;
                    break;
                }
            }
        }
        notifyUpdated();
    }

    public synchronized void clearConversation() {
        messages.removeAllElements();
        notifyUpdated();
    }

    public SmartBridgeApp getApp() {
        return app;
    }

    private String formatCurrentTime() {
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        return (h < 10 ? "0" + h : "" + h) + ":" + (m < 10 ? "0" + m : "" + m);
    }
}
