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
     * "SMS_INCOMING|<numéro>|<nomContact>|<slotSIM>|<timestamp>|<base64Texte>\n"
     */
    public void handleIncomingSms(String number, String contactName, String slotStr, String timestamp, String base64Text) {
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

        // Décodage Base64 du corps du texte
        String clearBody = FastBase64.decodeString(base64Text);
        if (clearBody == null || clearBody.length() == 0) {
            clearBody = base64Text; // Fallback si texte brut
        }

        String displayTime = (timestamp != null && timestamp.trim().length() > 0) ? timestamp : formatCurrentTime();
        final SmsItem item = new SmsItem(
            "inc_" + System.currentTimeMillis(),
            number,
            contactName,
            slot,
            simName,
            displayTime,
            clearBody,
            false
        );

        synchronized (this) {
            messages.insertElementAt(item, 0);
            if (messages.size() > 60) {
                messages.removeElementAt(messages.size() - 1);
            }
        }

        // Déclencher vibration physique, sonnerie et LED rouge clignotante BlackBerry
        HardwareManager.triggerSmsAlert();

        notifyUpdated();
        notifyNewMessage(item);

        // Afficher l'alerte à l'écran
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    String senderDisplay = item.getDisplayName();
                    Dialog.inform("Nouveau SMS reçu de : " + senderDisplay + "\n[" + item.simName + "]\n\n\"" + item.getSnippet(80) + "\"");
                } catch (Throwable t) {
                    System.out.println("[BB ERROR] " + t.getMessage());
                }
            }
        });
    }

    /**
     * Envoie un SMS rédigé au clavier :
     * "SMS_SEND|<numéroDestinataire>|<base64Texte>|<slotSIM>\n"
     */
    public void sendSms(final String recipient, final String text, final int slotSim) {
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
        final String base64Text = FastBase64.encodeString(text);

        String simName = "SIM " + (slotSim + 1);
        try {
            if (app.getCallManager() != null) {
                SimCard sim = app.getCallManager().getSim(slotSim);
                if (sim != null && sim.getName() != null) {
                    simName = sim.getName();
                }
            }
        } catch (Throwable ignored) {}

        final SmsItem outgoingItem = new SmsItem(
            "out_" + System.currentTimeMillis(),
            cleanRecipient,
            cleanRecipient,
            slotSim,
            simName,
            formatCurrentTime(),
            text,
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
                    String packet = "SMS_SEND|" + cleanRecipient + "|" + base64Text + "|" + slotSim + "\n";
                    LogManager.log("SMS", "Sending SMS packet: " + cleanRecipient + " (slot " + slotSim + ")");
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
     * Accusé d'envoi reçu d'Android : "SMS_SENT_OK|<numéro>|<simName>\n"
     */
    public void handleSmsSentOk(final String number, final String simName) {
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
                    Dialog.inform("SMS envoyé avec succès à " + number + " (" + (simName != null ? simName : "SIM") + ")");
                } catch (Throwable ignored) {}
            }
        });
    }

    /**
     * Accusé d'échec reçu d'Android : "SMS_SENT_ERROR|<numéro>|<raison>\n"
     */
    public void handleSmsSentError(final String number, final String reason) {
        synchronized (this) {
            for (int i = 0; i < messages.size(); i++) {
                SmsItem item = (SmsItem) messages.elementAt(i);
                if (item.isOutgoing && (number == null || number.length() == 0 || item.senderNumber.indexOf(number) >= 0 || number.indexOf(item.senderNumber) >= 0)) {
                    item.status = "Échec: " + (reason != null ? reason : "Erreur réseau");
                    break;
                }
            }
        }
        notifyUpdated();

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    Dialog.alert("Erreur d'envoi SMS vers " + number + " : " + (reason != null ? reason : "Échec réseau"));
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
