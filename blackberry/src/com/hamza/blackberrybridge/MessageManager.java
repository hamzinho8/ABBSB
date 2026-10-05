package com.hamza.blackberrybridge;

import java.util.Calendar;
import java.util.Vector;
import com.hamza.blackberrybridge.audio.FastBase64;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

/**
 * Gestionnaire de messagerie SMS pour BlackBerry Curve 9300.
 * Gère la boîte de réception, l'envoi de SMS avec encodage Base64, le choix Double SIM
 * et la synchronisation avec le smartphone Android.
 */
public class MessageManager {
    private SmartBridgeApp app;
    private final Vector messages = new Vector();
    private final Vector listeners = new Vector();

    public interface MessageListener {
        void onMessagesUpdated();
        void onNewMessage(SmsItem item);
    }

    public MessageManager(SmartBridgeApp app) {
        this.app = app;
        seedInitialMessages();
    }

    private void seedInitialMessages() {
        messages.addElement(new SmsItem("sms_1", "+212634934134", "Amina Mansouri", 0, "inwi", "14:15", "Salut Hamza, as-tu reçu le document pour la réunion ?", false));
        messages.addElement(new SmsItem("sms_2", "+212655881230", "Youssef Bennani", 1, "Orange", "11:50", "Je suis en route, j'arrive dans 15 minutes.", false));
        messages.addElement(new SmsItem("sms_3", "220", "Service Client inwi", 0, "inwi", "Hier", "Votre solde de recharge est disponible. Composez *120# pour consulter vos unités.", false));
        messages.addElement(new SmsItem("sms_4", "+212611223344", "Hamza H.", 0, "inwi", "Hier", "Parfait, on valide le pont Bluetooth ce soir !", true));
        messages.addElement(new SmsItem("sms_5", "121", "Orange Info", 1, "Orange", "25 Sep", "Profitez du Pass Internet 4G en composant *121#.", false));
    }

    public synchronized Vector getMessages() {
        Vector copy = new Vector(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            copy.addElement(messages.elementAt(i));
        }
        return copy;
    }

    public synchronized void addListener(MessageListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.addElement(listener);
        }
    }

    public synchronized void removeListener(MessageListener listener) {
        if (listener != null) {
            listeners.removeElement(listener);
        }
    }

    private void notifyUpdated() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                for (int i = 0; i < listeners.size(); i++) {
                    try {
                        ((MessageListener) listeners.elementAt(i)).onMessagesUpdated();
                    } catch (Throwable ignored) {}
                }
            }
        });
    }

    private void notifyNewMessage(final SmsItem item) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                for (int i = 0; i < listeners.size(); i++) {
                    try {
                        ((MessageListener) listeners.elementAt(i)).onNewMessage(item);
                    } catch (Throwable ignored) {}
                }
            }
        });
    }

    /**
     * Traite un SMS entrant reçu d'Android :
     * "SMS_INCOMING|<numéro>|<nomContact>|<slotSIM>|<timestamp>|<texteEnClair>|<base64>\n"
     */
    public void handleIncomingSms(String number, String contactName, String slotStr, String timestamp, String plainText) {
        int slot = 0;
        try {
            if (slotStr != null && slotStr.trim().length() > 0) {
                slot = Integer.parseInt(slotStr.trim());
            }
        } catch (Exception ignored) {}

        String simName = "SIM " + (slot + 1);
        try {
            if (app.getCallManager() != null) {
                SimCard sim = app.getCallManager().getSim(slot);
                if (sim != null && sim.getName() != null && sim.getName().length() > 0) {
                    simName = sim.getName();
                }
            }
        } catch (Throwable ignored) {}

        String body = (plainText != null) ? plainText : "";
        String displayTime = (timestamp != null && timestamp.trim().length() > 0) ? timestamp : formatCurrentTime();
        final SmsItem item = new SmsItem(
            "inc_" + System.currentTimeMillis(),
            number,
            (contactName != null && contactName.length() > 0) ? contactName : number,
            slot,
            simName,
            displayTime,
            body,
            false
        );

        synchronized (this) {
            messages.insertElementAt(item, 0);
            if (messages.size() > 100) {
                messages.removeElementAt(messages.size() - 1);
            }
        }

        // Déclencher vibration physique, sonnerie et LED clignotante BlackBerry
        try {
            HardwareManager.triggerSmsAlert();
        } catch (Throwable ignored) {}

        notifyUpdated();
        notifyNewMessage(item);
    }

    public void handleIncomingSmsMsg(String number, String contactName, String plainText, String time) {
        handleIncomingSms(number, contactName, "0", time, plainText);
    }

    public void handleSmsItem(boolean isOutgoing, String address, String contactName, String timestamp, String plainText) {
        final SmsItem item = new SmsItem(
            "item_" + address + "_" + timestamp,
            address,
            (contactName != null && contactName.length() > 0) ? contactName : address,
            0,
            "SIM 1",
            timestamp != null ? timestamp : formatCurrentTime(),
            plainText != null ? plainText : "",
            isOutgoing
        );
        synchronized (this) {
            boolean exists = false;
            for (int i = 0; i < messages.size(); i++) {
                SmsItem m = (SmsItem) messages.elementAt(i);
                if (m.senderNumber.equals(item.senderNumber) && m.body.equals(item.body) && m.timestamp.equals(item.timestamp)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                messages.addElement(item);
            }
        }
        notifyUpdated();
    }

    public void handleSmsListEnd(String totalCount) {
        notifyUpdated();
    }

    /**
     * Envoie un SMS rédigé au clavier en clair sans encodage Base64 :
     * "SMS_SEND|" + numeroDestinataire + "|" + messageTexteEnClair + "\n"
     */
    public void sendSms(final String recipient, final String text, final int slotSim) {
        sendSms(recipient, text);
    }

    public void sendSms(final String recipient, final String text) {
        if (recipient == null || recipient.trim().length() == 0) {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() { Dialog.alert("Numéro de destinataire manquant."); }
            });
            return;
        }
        if (text == null || text.trim().length() == 0) {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() { Dialog.alert("Le message SMS ne peut pas être vide."); }
            });
            return;
        }

        final String cleanRecipient = recipient.trim();
        final String cleanText = text.trim();

        final SmsItem outgoingItem = new SmsItem(
            "out_" + System.currentTimeMillis(),
            cleanRecipient,
            cleanRecipient,
            0,
            "SIM 1",
            formatCurrentTime(),
            cleanText,
            true
        );
        outgoingItem.status = "En cours d'envoi...";

        synchronized (this) {
            messages.insertElementAt(outgoingItem, 0);
        }
        notifyUpdated();

        // Envoi dans un thread en arrière-plan sans bloquer l'Event Dispatch Thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    String packet = "SMS_SEND|" + cleanRecipient + "|" + cleanText + "\n";
                    LogManager.log("SMS", "Sending plain text SMS packet: " + packet.trim());
                    app.getConnectionManager().sendData(packet);
                } catch (Throwable t) {
                    LogManager.error("SMS", "Send error: " + t.getMessage());
                    outgoingItem.status = "Échec d'envoi";
                    notifyUpdated();
                }
            }
        }).start();
    }

    /**
     * Accusé d'envoi reçu d'Android : "SMS_SENT_OK|<numéro>|<simName>|<contactName>\n"
     */
    public void handleSmsSentOk(final String number, final String simName, final String contactName) {
        final String displayName = (contactName != null && contactName.length() > 0) ? contactName : number;
        synchronized (this) {
            for (int i = 0; i < messages.size(); i++) {
                SmsItem item = (SmsItem) messages.elementAt(i);
                if (item.isOutgoing && (number == null || number.length() == 0 || item.senderNumber.indexOf(number) >= 0 || number.indexOf(item.senderNumber) >= 0)) {
                    item.status = "Envoyé (OK)";
                    if (simName != null && simName.length() > 0) {
                        item.simName = simName;
                    }
                    break;
                }
            }
        }
        notifyUpdated();

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    Dialog.inform("SMS envoyé avec succès à " + displayName);
                } catch (Throwable ignored) {}
            }
        });
    }

    public void handleSmsSentOk(final String number, final String simName) {
        handleSmsSentOk(number, simName, number);
    }

    /**
     * Accusé d'échec reçu d'Android : "SMS_SENT_ERROR|<numéro>|<raison>\n"
     */
    public void handleSmsSentError(final String number, final String reason) {
        final String err = (reason != null && reason.length() > 0) ? reason : "Échec d'envoi";
        synchronized (this) {
            for (int i = 0; i < messages.size(); i++) {
                SmsItem item = (SmsItem) messages.elementAt(i);
                if (item.isOutgoing && (number == null || number.length() == 0 || item.senderNumber.indexOf(number) >= 0 || number.indexOf(item.senderNumber) >= 0)) {
                    item.status = "Échec d'envoi";
                    break;
                }
            }
        }
        notifyUpdated();

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    Dialog.alert("Échec envoi SMS : " + err);
                } catch (Throwable ignored) {}
            }
        });
    }

    /**
     * Demande de synchronisation des SMS récents : "SMS_GET_RECENT|20\n"
     */
    public void requestRecentSms() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    LogManager.log("SMS", "Requesting recent SMS sync: SMS_GET_RECENT|20");
                    app.getConnectionManager().sendData("SMS_GET_RECENT|20\n");
                } catch (Throwable t) {
                    LogManager.error("SMS", "Sync error: " + t.getMessage());
                }
            }
        }).start();
    }

    public synchronized void deleteMessage(SmsItem item) {
        if (item != null) {
            messages.removeElement(item);
            notifyUpdated();
        }
    }

    public synchronized void clearAllMessages() {
        messages.removeAllElements();
        notifyUpdated();
    }

    private String formatCurrentTime() {
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        return (h < 10 ? "0" + h : "" + h) + ":" + (m < 10 ? "0" + m : "" + m);
    }
}
