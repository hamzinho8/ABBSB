package com.hamza.blackberrybridge;

import java.util.Calendar;
import java.util.Vector;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.system.Characters;
import net.rim.device.api.ui.Keypad;

/**
 * SmsHubScreen - Boîte de Réception et Gestionnaire de SMS pour BlackBerry Curve 9300.
 * Compatible RIM OS 5.0, 6.0 et 7.1 (CLDC 1.1 / MIDP 2.0).
 *
 * Spécifications Bluetooth SPP :
 * 1. Envoi SMS : "SMS_SEND|" + numeroDestinataire + "|" + messageTexteEnClair + "\n"
 * 2. Réception en direct : "SMS_INCOMING|num|nom|slot|time|texteClair|b64" ou "SMS_MSG|num|nom|texteClair|time"
 * 3. Historique récent : "SMS_GET_RECENT|20\n" -> "SMS_ITEM|type|adresse|nom|time|texteClair|b64"
 * 4. Accusés : "SMS_SENT_OK|num|sim|contact" et "SMS_SENT_ERROR|num|raison"
 *
 * Fonctionnalités matérielles BlackBerry Curve 9300 :
 * - Défilement fluide et sélection à la Trackpad optique (écran 320x240).
 * - Touche Verte (KEY_SEND) : Appel direct et immédiat du contact sélectionné.
 * - Touche Rouge (KEY_END) : Fermeture de l'écran.
 * - Validation par la touche Entrée (Characters.ENTER) pour l'envoi rapide.
 */
public class SmsHubScreen extends MainScreen implements MessageManager.MessageListener {
    private SmartBridgeApp app;
    private final Vector messages = new Vector();
    private VerticalFieldManager listContainer;
    private DarkLabelField countLabel;
    private DarkLabelField statusLabel;

    public SmsHubScreen() {
        this(null);
    }

    public SmsHubScreen(SmartBridgeApp app) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        
        if (app != null) {
            this.app = app;
        } else {
            UiApplication currentApp = UiApplication.getUiApplication();
            if (currentApp instanceof SmartBridgeApp) {
                this.app = (SmartBridgeApp) currentApp;
            }
        }

        // Fond sombre pur Curve 9300
        try {
            getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        } catch (Throwable ignored) {}

        VerticalFieldManager root = new VerticalFieldManager(Field.FIELD_HCENTER);
        root.setPadding(3, 6, 3, 6);

        // 1. Bandeau supérieur Titre
        VerticalFieldManager headerBox = new VerticalFieldManager(Field.FIELD_HCENTER);
        headerBox.setBackground(BackgroundFactory.createSolidBackground(0x0F172A));
        headerBox.setPadding(3, 8, 3, 8);

        DarkLabelField title = new DarkLabelField("MESSAGES SMS", Field.FIELD_HCENTER, 0x00E5FF);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch (Throwable ignored) {}
        headerBox.add(title);

        countLabel = new DarkLabelField("Boîte de réception (0 message)", Field.FIELD_HCENTER, 0x94A3B8);
        try { countLabel.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
        headerBox.add(countLabel);
        root.add(headerBox);

        // 2. Barre de boutons d'action rapide
        HorizontalFieldManager actionBtns = new HorizontalFieldManager(Field.FIELD_HCENTER);
        actionBtns.setPadding(3, 0, 3, 0);

        DarkButtonField btnNew = new DarkButtonField("Nouveau SMS", 100, 24, DarkButtonField.STYLE_GREEN);
        btnNew.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                openComposeScreen("", "");
            }
        });

        DarkButtonField btnSync = new DarkButtonField("Actualiser", 85, 24, DarkButtonField.STYLE_CYAN);
        btnSync.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                requestRecentSms();
            }
        });

        DarkButtonField btnDialer = new DarkButtonField("Clavier", 75, 24, DarkButtonField.STYLE_SLATE);
        btnDialer.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                if (getApp() != null && getApp().getUIManager() != null) {
                    getApp().getUIManager().openPhoneDialer();
                }
            }
        });

        actionBtns.add(btnNew);
        actionBtns.add(btnSync);
        actionBtns.add(btnDialer);
        root.add(actionBtns);

        // 3. Indicateur d'état interactif
        statusLabel = new DarkLabelField("Touche Verte: Appeler | Trackpad: Sélectionner | Menu: Options", Field.FIELD_HCENTER, 0x64748B);
        try { statusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch (Throwable ignored) {}
        root.add(statusLabel);

        // 4. Liste scrollable des SMS
        listContainer = new VerticalFieldManager(Field.FIELD_HCENTER);
        listContainer.setPadding(2, 0, 4, 0);
        root.add(listContainer);

        add(root);

        // Initialisation des données depuis MessageManager si disponible
        loadInitialMessages();

        // Enregistrement comme écran actif dans UIManager
        if (getApp() != null && getApp().getUIManager() != null) {
            getApp().getUIManager().setSmsHubScreen(this);
        }
        if (getApp() != null && getApp().getMessageManager() != null) {
            getApp().getMessageManager().addListener(this);
        }

        rebuildMessagesList();
    }

    private SmartBridgeApp getApp() {
        if (app == null) {
            UiApplication current = UiApplication.getUiApplication();
            if (current instanceof SmartBridgeApp) {
                app = (SmartBridgeApp) current;
            }
        }
        return app;
    }

    private void loadInitialMessages() {
        if (getApp() != null && getApp().getMessageManager() != null) {
            Vector managerMsgs = getApp().getMessageManager().getMessages();
            synchronized (messages) {
                messages.removeAllElements();
                for (int i = 0; i < managerMsgs.size(); i++) {
                    messages.addElement(managerMsgs.elementAt(i));
                }
            }
        } else {
            // Messages de démonstration si standalone
            synchronized (messages) {
                messages.addElement(new SmsItem("sms_1", "+212634934134", "Amina Mansouri", 0, "inwi", "14:15", "Salut Hamza, as-tu reçu le document pour la réunion ?", false));
                messages.addElement(new SmsItem("sms_2", "+212655881230", "Youssef Bennani", 1, "Orange", "11:50", "Je suis en route, j'arrive dans 15 minutes.", false));
                messages.addElement(new SmsItem("sms_3", "220", "Service Client inwi", 0, "inwi", "Hier", "Votre solde de recharge est disponible. Composez *120# pour consulter vos unités.", false));
                messages.addElement(new SmsItem("sms_4", "+212611223344", "Hamza H.", 0, "inwi", "Hier", "Parfait, on valide le pont Bluetooth ce soir !", true));
            }
        }
    }

    public boolean onClose() {
        if (getApp() != null && getApp().getUIManager() != null) {
            if (getApp().getUIManager().getSmsHubScreen() == this) {
                getApp().getUIManager().setSmsHubScreen(null);
            }
        }
        if (getApp() != null && getApp().getMessageManager() != null) {
            getApp().getMessageManager().removeListener(this);
        }
        return super.onClose();
    }

    public void onMessagesUpdated() {
        loadInitialMessages();
        rebuildMessagesList();
    }

    public void onNewMessage(SmsItem item) {
        if (item != null) {
            synchronized (messages) {
                if (!messages.contains(item)) {
                    messages.insertElementAt(item, 0);
                }
            }
        }
        rebuildMessagesList();
    }

    // =========================================================================
    // PARSER PRINCIPAL DU PROTOCOLE BLUETOOTH SPP
    // =========================================================================

    /**
     * Traite un paquet entrant d'Android.
     * Robuste face aux sauts de ligne (\r, \n) et aux paquets multilignes.
     */
    public void parseIncomingPacket(final String packet) {
        if (packet == null || packet.trim().length() == 0) return;

        // Découpage par ligne pour traiter les envois groupés
        String[] lines = split(packet, '\n');
        for (int l = 0; l < lines.length; l++) {
            String rawLine = lines[l];
            if (rawLine == null) continue;
            rawLine = rawLine.trim();
            if (rawLine.length() == 0) continue;
            if (rawLine.endsWith("\r")) {
                rawLine = rawLine.substring(0, rawLine.length() - 1);
            }

            final String line = rawLine;
            final String[] parts = split(line, '|');
            if (parts.length == 0) continue;

            final String command = parts[0];

            // 1. SMS_INCOMING en direct
            // Format : "SMS_INCOMING|<numéro>|<nomContact>|<slotSIM>|<timestamp>|<texteEnClair>|<base64>\n"
            if (command.equals("SMS_INCOMING")) {
                final String number = (parts.length >= 2) ? parts[1].trim() : "";
                final String contactName = (parts.length >= 3 && parts[2].trim().length() > 0) ? parts[2].trim() : number;
                final String slotStr = (parts.length >= 4) ? parts[3].trim() : "0";
                final String timestamp = (parts.length >= 5 && parts[4].trim().length() > 0) ? parts[4].trim() : formatCurrentTime();
                
                // Le texte en clair est en parts[5] selon spécifications strictes
                String textBody = "";
                if (parts.length >= 6) {
                    textBody = parts[5];
                } else if (parts.length >= 4) {
                    textBody = parts[parts.length - 1];
                }

                final String finalText = textBody;
                int slot = 0;
                try { slot = Integer.parseInt(slotStr); } catch (Exception ignored) {}
                final String simName = "SIM " + (slot + 1);

                final SmsItem incomingItem = new SmsItem(
                    "inc_" + System.currentTimeMillis(),
                    number,
                    contactName,
                    slot,
                    simName,
                    timestamp,
                    finalText,
                    false
                );

                synchronized (messages) {
                    messages.insertElementAt(incomingItem, 0);
                    if (messages.size() > 100) {
                        messages.removeElementAt(messages.size() - 1);
                    }
                }

                // Alerte matérielle Curve 9300 : vibration, sonnerie et LED clignotante
                try {
                    HardwareManager.triggerSmsAlert();
                } catch (Throwable ignored) {}

                // Mise à jour de l'UI
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        statusLabel.setText("Nouveau SMS de " + incomingItem.getDisplayName());
                        statusLabel.setColor(0x22C55E);
                        rebuildMessagesList();
                        try {
                            Dialog.inform("Nouveau SMS reçu de : " + incomingItem.getDisplayName() + "\n\n\"" + incomingItem.getSnippet(100) + "\"");
                        } catch (Throwable ignored) {}
                    }
                });
            }

            // 2. SMS_MSG en direct (format alternatif)
            // Format : "SMS_MSG|<numéro>|<nomContact>|<texteEnClair>|<timestamp>\n"
            else if (command.equals("SMS_MSG")) {
                final String number = (parts.length >= 2) ? parts[1].trim() : "";
                final String contactName = (parts.length >= 3 && parts[2].trim().length() > 0) ? parts[2].trim() : number;
                final String textBody = (parts.length >= 4) ? parts[3] : "";
                final String timestamp = (parts.length >= 5 && parts[4].trim().length() > 0) ? parts[4].trim() : formatCurrentTime();

                final SmsItem incomingItem = new SmsItem(
                    "msg_" + System.currentTimeMillis(),
                    number,
                    contactName,
                    0,
                    "SIM 1",
                    timestamp,
                    textBody,
                    false
                );

                synchronized (messages) {
                    messages.insertElementAt(incomingItem, 0);
                }

                try {
                    HardwareManager.triggerSmsAlert();
                } catch (Throwable ignored) {}

                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        statusLabel.setText("Nouveau message de " + incomingItem.getDisplayName());
                        statusLabel.setColor(0x22C55E);
                        rebuildMessagesList();
                    }
                });
            }

            // 3. SMS_ITEM (Historique de la boîte de réception)
            // Format : "SMS_ITEM|<type: 1=reçu, 2=envoyé>|<adresse>|<nomContact>|<timestamp>|<texteEnClair>|<base64>\n"
            else if (command.equals("SMS_ITEM")) {
                int type = 1;
                try {
                    if (parts.length >= 2) type = Integer.parseInt(parts[1].trim());
                } catch (Exception ignored) {}
                final boolean isOutgoing = (type == 2);

                final String address = (parts.length >= 3) ? parts[2].trim() : "";
                final String contactName = (parts.length >= 4 && parts[3].trim().length() > 0) ? parts[3].trim() : address;
                final String timestamp = (parts.length >= 5) ? parts[4].trim() : formatCurrentTime();
                
                String textBody = "";
                if (parts.length >= 6) {
                    textBody = parts[5];
                } else if (parts.length >= 4) {
                    textBody = parts[parts.length - 1];
                }

                final SmsItem historyItem = new SmsItem(
                    "hist_" + address + "_" + timestamp,
                    address,
                    contactName,
                    0,
                    "SIM 1",
                    timestamp,
                    textBody,
                    isOutgoing
                );

                synchronized (messages) {
                    // Éviter les doublons stricts
                    boolean exists = false;
                    for (int i = 0; i < messages.size(); i++) {
                        SmsItem existing = (SmsItem) messages.elementAt(i);
                        if (existing.senderNumber.equals(historyItem.senderNumber) && 
                            existing.body.equals(historyItem.body) && 
                            existing.timestamp.equals(historyItem.timestamp)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        messages.addElement(historyItem);
                    }
                }

                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        rebuildMessagesList();
                    }
                });
            }

            // 4. SMS_LIST_END (Fin du lot d'historique)
            else if (command.equals("SMS_LIST_END")) {
                final String totalStr = (parts.length >= 2) ? parts[1].trim() : "" + messages.size();
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        statusLabel.setText("Historique à jour (" + totalStr + " SMS)");
                        statusLabel.setColor(0x22C55E);
                        rebuildMessagesList();
                    }
                });
            }

            // 5. Accusé de réception succès : "SMS_SENT_OK|<numéro>|<simName>|<contactName>\n"
            else if (command.equals("SMS_SENT_OK")) {
                final String number = (parts.length >= 2) ? parts[1].trim() : "";
                final String simName = (parts.length >= 3) ? parts[2].trim() : "SIM 1";
                final String contactName = (parts.length >= 4 && parts[3].trim().length() > 0) ? parts[3].trim() : number;

                synchronized (messages) {
                    for (int i = 0; i < messages.size(); i++) {
                        SmsItem item = (SmsItem) messages.elementAt(i);
                        if (item.isOutgoing && (number.length() == 0 || item.senderNumber.indexOf(number) >= 0 || number.indexOf(item.senderNumber) >= 0)) {
                            item.status = "Envoyé (OK)";
                            item.simName = simName;
                            break;
                        }
                    }
                }

                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        statusLabel.setText("SMS délivré à " + contactName);
                        statusLabel.setColor(0x22C55E);
                        rebuildMessagesList();
                        try {
                            Dialog.inform("SMS envoyé avec succès à " + contactName);
                        } catch (Throwable ignored) {}
                    }
                });
            }

            // 6. Accusé d'échec : "SMS_SENT_ERROR|<numéro>|<raison>\n"
            else if (command.equals("SMS_SENT_ERROR")) {
                final String number = (parts.length >= 2) ? parts[1].trim() : "";
                final String reason = (parts.length >= 3) ? parts[2].trim() : "Erreur inconnue";

                synchronized (messages) {
                    for (int i = 0; i < messages.size(); i++) {
                        SmsItem item = (SmsItem) messages.elementAt(i);
                        if (item.isOutgoing && (number.length() == 0 || item.senderNumber.indexOf(number) >= 0 || number.indexOf(item.senderNumber) >= 0)) {
                            item.status = "Échec d'envoi";
                            break;
                        }
                    }
                }

                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        statusLabel.setText("Échec SMS vers " + number);
                        statusLabel.setColor(0xEF4444);
                        rebuildMessagesList();
                        try {
                            Dialog.alert("Échec envoi SMS : " + reason);
                        } catch (Throwable ignored) {}
                    }
                });
            }
        }
    }

    // =========================================================================
    // ENVOI DE SMS & SYNCHRONISATION BLUETOOTH
    // =========================================================================

    /**
     * Envoie un SMS à destination d'Android selon le protocole :
     * "SMS_SEND|" + numeroDestinataire + "|" + messageTexteEnClair + "\n"
     * Sans encodage Base64, Android gère l'envoi et la sauvegarde.
     */
    public void sendSms(final String recipient, final String text) {
        if (recipient == null || recipient.trim().length() == 0) {
            Dialog.alert("Numéro de destinataire manquant.");
            return;
        }
        if (text == null || text.trim().length() == 0) {
            Dialog.alert("Le message SMS ne peut pas être vide.");
            return;
        }

        final String cleanNumber = recipient.trim();
        final String cleanText = text.trim();

        // 1. Ajout immédiat dans la liste locale avec état "En cours d'envoi..."
        final SmsItem outgoing = new SmsItem(
            "out_" + System.currentTimeMillis(),
            cleanNumber,
            cleanNumber,
            0,
            "SIM 1",
            formatCurrentTime(),
            cleanText,
            true
        );
        outgoing.status = "En cours d'envoi...";

        synchronized (messages) {
            messages.insertElementAt(outgoing, 0);
        }
        rebuildMessagesList();

        statusLabel.setText("Envoi SMS vers " + cleanNumber + "...");
        statusLabel.setColor(0xFACC15);

        // 2. Émission Bluetooth asynchrone sans bloquer l'Event Dispatch Thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    final String packet = "SMS_SEND|" + cleanNumber + "|" + cleanText + "\n";
                    LogManager.log("SMS", "Sending Bluetooth SPP packet: " + packet.trim());

                    if (getApp() != null && getApp().getConnectionManager() != null) {
                        getApp().getConnectionManager().sendData(packet);
                    } else {
                        // Simulation locale si non connecté
                        UiApplication.getUiApplication().invokeLater(new Runnable() {
                            public void run() {
                                parseIncomingPacket("SMS_SENT_OK|" + cleanNumber + "|SIM 1|" + cleanNumber + "\n");
                            }
                        });
                    }
                } catch (Throwable t) {
                    LogManager.error("SMS", "Send error: " + t.getMessage());
                    outgoing.status = "Échec d'envoi";
                    UiApplication.getUiApplication().invokeLater(new Runnable() {
                        public void run() {
                            statusLabel.setText("Erreur d'émission Bluetooth");
                            statusLabel.setColor(0xEF4444);
                            rebuildMessagesList();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Demande à Android de synchroniser les 20 derniers SMS :
     * Format : "SMS_GET_RECENT|20\n"
     */
    public void requestRecentSms() {
        statusLabel.setText("Synchronisation Bluetooth (SMS_GET_RECENT|20)...");
        statusLabel.setColor(0xFACC15);

        new Thread(new Runnable() {
            public void run() {
                try {
                    String req = "SMS_GET_RECENT|20\n";
                    LogManager.log("SMS", "Requesting recent SMS: " + req.trim());
                    if (getApp() != null && getApp().getConnectionManager() != null) {
                        getApp().getConnectionManager().sendData(req);
                    } else {
                        // Simulation de synchronisation si standalone
                        UiApplication.getUiApplication().invokeLater(new Runnable() {
                            public void run() {
                                statusLabel.setText("Synchronisation réussie (Mode local)");
                                statusLabel.setColor(0x22C55E);
                            }
                        });
                    }
                } catch (Throwable t) {
                    LogManager.error("SMS", "Sync error: " + t.getMessage());
                }
            }
        }).start();
    }

    // =========================================================================
    // NAVIGATION & INTERACTION UTILISATEUR
    // =========================================================================

    public void openComposeScreen(String prefillNumber, String prefillName) {
        if (getApp() != null && getApp().getUIManager() != null) {
            getApp().getUIManager().pushScreen(new ComposeSmsScreen(getApp(), prefillNumber, prefillName));
        } else {
            // Boîte de dialogue intégrée rapide
            showQuickComposePopup(prefillNumber);
        }
    }

    private void showQuickComposePopup(String prefill) {
        final EditField phoneField = new EditField("N° : ", prefill != null ? prefill : "", 25, BasicEditField.FILTER_PHONE);
        final EditField msgField = new EditField("Message : ", "", 400, BasicEditField.NO_NEWLINE) {
            protected boolean keyChar(char ch, int status, int time) {
                if (ch == Characters.ENTER || ch == '\n') {
                    String p = phoneField.getText().trim();
                    String m = getText().trim();
                    if (p.length() > 0 && m.length() > 0) {
                        sendSms(p, m);
                        UiApplication.getUiApplication().popScreen(UiApplication.getUiApplication().getActiveScreen());
                        return true;
                    }
                }
                return super.keyChar(ch, status, time);
            }
        };

        PopupScreen popup = new PopupScreen(new VerticalFieldManager());
        popup.add(new LabelField("Rédiger un SMS"));
        popup.add(phoneField);
        popup.add(msgField);

        ButtonField btnSend = new ButtonField("Envoyer (Entrée)", ButtonField.CONSUME_CLICK);
        btnSend.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                String p = phoneField.getText().trim();
                String m = msgField.getText().trim();
                if (p.length() > 0 && m.length() > 0) {
                    sendSms(p, m);
                    UiApplication.getUiApplication().popScreen(UiApplication.getUiApplication().getActiveScreen());
                } else {
                    Dialog.alert("Champs incomplets.");
                }
            }
        });
        popup.add(btnSend);

        UiApplication.getUiApplication().pushScreen(popup);
    }

    private void rebuildMessagesList() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    listContainer.deleteAll();
                    countLabel.setText("Boîte : " + messages.size() + " message(s)");

                    if (messages.isEmpty()) {
                        VerticalFieldManager emptyBox = new VerticalFieldManager(Field.FIELD_HCENTER);
                        emptyBox.setPadding(14, 10, 14, 10);
                        DarkLabelField emptyLbl = new DarkLabelField("Aucun SMS dans la boîte", Field.FIELD_HCENTER, 0x64748B);
                        emptyBox.add(emptyLbl);
                        listContainer.add(emptyBox);
                        return;
                    }

                    for (int i = 0; i < messages.size(); i++) {
                        final SmsItem sms = (SmsItem) messages.elementAt(i);
                        listContainer.add(createSmsCard(sms));
                    }
                } catch (Throwable t) {
                    System.out.println("[BB ERROR] " + t.getMessage());
                }
            }
        });
    }

    private Field createSmsCard(final SmsItem sms) {
        SmsCardField card = new SmsCardField(sms);
        card.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                openSmsDetailDialog(sms);
            }
        });
        return card;
    }

    private void openSmsDetailDialog(final SmsItem sms) {
        String title = (sms.isOutgoing ? "SMS Envoyé à " : "SMS de ") + sms.getDisplayName();
        String body = "[" + sms.simName + " | " + sms.timestamp + "]\nStatut: " + sms.status + "\n\n" + sms.body;

        Object[] options = new Object[] {
            "Appeler (Touche Verte)",
            "Répondre par SMS",
            "Supprimer",
            "Fermer"
        };

        int choice = Dialog.ask(title + "\n\n" + body, options, 0);
        if (choice == 0) {
            callContactOfSms(sms);
        } else if (choice == 1) {
            openComposeScreen(sms.senderNumber, sms.senderName);
        } else if (choice == 2) {
            synchronized (messages) {
                messages.removeElement(sms);
            }
            if (getApp() != null && getApp().getMessageManager() != null) {
                getApp().getMessageManager().deleteMessage(sms);
            }
            rebuildMessagesList();
        }
    }

    private void callContactOfSms(SmsItem sms) {
        if (sms != null && sms.senderNumber != null && sms.senderNumber.length() > 0) {
            statusLabel.setText("Appel en cours : " + sms.getDisplayName() + "...");
            statusLabel.setColor(0x22C55E);
            if (getApp() != null && getApp().getCallManager() != null) {
                getApp().getCallManager().initiateOutboundCall(sms.senderNumber, sms.senderName);
            } else {
                Dialog.inform("Appel vers " + sms.getDisplayName() + " (" + sms.senderNumber + ")");
            }
        } else {
            Dialog.alert("Numéro de contact introuvable.");
        }
    }

    private SmsItem getFocusedSms() {
        Field focused = getLeafFieldWithFocus();
        if (focused instanceof SmsCardField) {
            return ((SmsCardField) focused).getSms();
        }
        synchronized (messages) {
            if (!messages.isEmpty()) {
                return (SmsItem) messages.elementAt(0);
            }
        }
        return null;
    }

    // =========================================================================
    // Raccourcis physiques BlackBerry Curve 9300
    // =========================================================================

    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);

        // 1. Touche Appel Verte (KEY_SEND) :
        // Déclenche l'appel vocal direct vers le contact du SMS actuellement focalisé !
        if (key == Keypad.KEY_SEND) {
            SmsItem current = getFocusedSms();
            if (current != null) {
                callContactOfSms(current);
                return true;
            }
            return true;
        }

        // 2. Touche Fin d'appel Rouge (KEY_END) ou Échap :
        // Ferme immédiatement l'écran
        if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyDown(keycode, time);
    }

    // =========================================================================
    // Menu BlackBerry OS 6.0
    // =========================================================================

    protected void makeMenu(Menu menu, int instance) {
        menu.add(new MenuItem("Nouveau SMS", 100, 10) {
            public void run() {
                openComposeScreen("", "");
            }
        });

        menu.add(new MenuItem("Actualiser les SMS", 100, 20) {
            public void run() {
                requestRecentSms();
            }
        });

        final SmsItem focused = getFocusedSms();
        if (focused != null) {
            menu.add(new MenuItem("Appeler ce contact (Touche Verte)", 100, 30) {
                public void run() {
                    callContactOfSms(focused);
                }
            });

            menu.add(new MenuItem("Répondre par SMS", 100, 40) {
                public void run() {
                    openComposeScreen(focused.senderNumber, focused.senderName);
                }
            });

            menu.add(new MenuItem("Supprimer ce SMS", 100, 50) {
                public void run() {
                    synchronized (messages) {
                        messages.removeElement(focused);
                    }
                    if (getApp() != null && getApp().getMessageManager() != null) {
                        getApp().getMessageManager().deleteMessage(focused);
                    }
                    rebuildMessagesList();
                }
            });
        }

        menu.add(new MenuItem("Vider la boîte", 100, 60) {
            public void run() {
                if (Dialog.ask(Dialog.D_YES_NO, "Supprimer tous les SMS de la boîte ?") == Dialog.YES) {
                    synchronized (messages) {
                        messages.removeAllElements();
                    }
                    if (getApp() != null && getApp().getMessageManager() != null) {
                        getApp().getMessageManager().clearAllMessages();
                    }
                    rebuildMessagesList();
                }
            }
        });

        super.makeMenu(menu, instance);
    }

    // =========================================================================
    // DÉCOUPAGE DE CHAÎNES SANS REGEX (CLDC 1.1 / J2ME)
    // =========================================================================

    private static String[] split(String str, char delim) {
        if (str == null) return new String[0];
        Vector nodes = new Vector();
        int start = 0;
        int idx = str.indexOf(delim);
        while (idx >= 0) {
            nodes.addElement(str.substring(start, idx));
            start = idx + 1;
            idx = str.indexOf(delim, start);
        }
        nodes.addElement(str.substring(start));

        String[] res = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            res[i] = (String) nodes.elementAt(i);
        }
        return res;
    }

    private String formatCurrentTime() {
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        return (h < 10 ? "0" + h : "" + h) + ":" + (m < 10 ? "0" + m : "" + m);
    }

    // =========================================================================
    // Composant graphique de carte SMS (Optimisé Trackpad 320x240)
    // =========================================================================

    private static class SmsCardField extends Field {
        private SmsItem sms;

        public SmsCardField(SmsItem sms) {
            super(Field.FOCUSABLE);
            this.sms = sms;
        }

        public SmsItem getSms() {
            return sms;
        }

        public int getPreferredWidth() { return 308; }
        public int getPreferredHeight() { return 46; }

        protected void layout(int width, int height) {
            setExtent(Math.min(width, getPreferredWidth()), getPreferredHeight());
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            boolean focused = isFocus();

            // Fond avec surbrillance au trackpad
            if (focused) {
                g.setColor(0x0284C7); // Cyan vif actif
                g.fillRoundRect(0, 0, w, h, 6, 6);
                g.setColor(0x38BDF8);
                g.drawRoundRect(0, 0, w, h, 6, 6);
            } else {
                g.setColor(0x1E293B); // Anthracite sombre
                g.fillRoundRect(0, 0, w, h, 6, 6);
                g.setColor(0x334155);
                g.drawRoundRect(0, 0, w, h, 6, 6);
            }

            // Indicateur de sens : Vert pour reçu, Cyan pour envoyé
            int arrowColor = sms.isOutgoing ? 0x00E5FF : 0x22C55E;
            g.setColor(arrowColor);
            g.fillRoundRect(4, 6, 5, h - 12, 2, 2);

            // Ligne 1 : Expéditeur en GRAS
            g.setColor(Color.WHITE);
            try { g.setFont(Font.getDefault().derive(Font.BOLD, 12)); } catch (Throwable ignored) {}
            String name = sms.getDisplayName();
            if (name.length() > 20) name = name.substring(0, 19) + "..";
            g.drawText(name, 14, 4);

            // Ligne 1 Droite : Badge SIM / Date / Heure
            try { g.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
            g.setColor(focused ? 0xFEF08A : 0xFACC15);
            String tag = "[" + sms.simName + "] " + sms.timestamp;
            g.drawText(tag, w - 105, 4);

            // Ligne 2 : Extrait du message en clair
            g.setColor(focused ? Color.WHITE : 0xCBD5E1);
            try { g.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
            String snippet = sms.getSnippet(38);
            g.drawText(snippet, 14, 24);
        }

        protected boolean navigationClick(int status, int time) {
            fieldChangeNotify(0);
            return true;
        }

        protected boolean keyChar(char ch, int status, int time) {
            if (ch == Characters.ENTER || ch == ' ') {
                fieldChangeNotify(0);
                return true;
            }
            return super.keyChar(ch, status, time);
        }
    }
}
