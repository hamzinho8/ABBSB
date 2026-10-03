package com.hamza.blackberrybridge;

import java.util.Vector;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.system.Characters;

/**
 * Boîte de Réception et Gestionnaire de SMS pour BlackBerry Curve 9300 (OS 6.0).
 *
 * Exploite 100% du clavier physique et des touches dédiées :
 * - Touche Verte (KEY_SEND) : Appel direct et immédiat du contact sélectionné dans la liste des SMS.
 * - Touche Rouge (KEY_END) : Fermeture de l'écran.
 * - Touche Menu BlackBerry : Nouveau SMS, Répondre, Choix Double SIM, Actualiser.
 * - Trackpad optique : Navigation fluide et sélection de message.
 */
public class SmsHubScreen extends MainScreen implements MessageManager.MessageListener {
    private SmartBridgeApp app;
    private VerticalFieldManager listContainer;
    private DarkLabelField countLabel;
    private DarkLabelField statusLabel;

    public SmsHubScreen(final SmartBridgeApp app) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.app = app;

        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));

        VerticalFieldManager root = new VerticalFieldManager(Field.FIELD_HCENTER);
        root.setPadding(4, 6, 4, 6);

        // Header Title
        DarkLabelField title = new DarkLabelField("BOÎTE DE RÉCEPTION SMS", Field.FIELD_HCENTER, 0x00E5FF);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch (Throwable ignored) {}
        root.add(title);

        // Counter & Sync Status
        countLabel = new DarkLabelField("Chargement des messages...", Field.FIELD_HCENTER, 0x94A3B8);
        try { countLabel.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
        root.add(countLabel);

        // Top Action Buttons
        HorizontalFieldManager actionBtns = new HorizontalFieldManager(Field.FIELD_HCENTER);
        actionBtns.setPadding(3, 0, 4, 0);

        DarkButtonField btnNew = new DarkButtonField("Rédiger SMS", 95, 26, DarkButtonField.STYLE_GREEN);
        btnNew.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                app.getUIManager().pushScreen(new ComposeSmsScreen(app, "", ""));
            }
        });

        DarkButtonField btnSync = new DarkButtonField("Actualiser", 85, 26, DarkButtonField.STYLE_CYAN);
        btnSync.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                triggerSync();
            }
        });

        DarkButtonField btnDialer = new DarkButtonField("Clavier", 75, 26, DarkButtonField.STYLE_SLATE);
        btnDialer.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                app.getUIManager().pushScreen(new PhoneDialerScreen(app, app.getCallManager()));
            }
        });

        actionBtns.add(btnNew);
        actionBtns.add(btnSync);
        actionBtns.add(btnDialer);
        root.add(actionBtns);

        statusLabel = new DarkLabelField("Touche Verte: Appeler contact | Clic: Lire / Répondre", Field.FIELD_HCENTER, 0x64748B);
        try { statusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
        root.add(statusLabel);

        // SMS Messages Container
        listContainer = new VerticalFieldManager(Field.FIELD_HCENTER);
        listContainer.setPadding(2, 0, 4, 0);
        root.add(listContainer);

        add(root);

        // Register listener for real-time incoming SMS
        app.getMessageManager().addListener(this);

        // Populate initial messages
        rebuildMessagesList();
    }

    public boolean onClose() {
        if (app != null && app.getMessageManager() != null) {
            app.getMessageManager().removeListener(this);
        }
        return super.onClose();
    }

    public void onMessagesUpdated() {
        rebuildMessagesList();
    }

    public void onNewMessage(SmsItem item) {
        rebuildMessagesList();
    }

    private void triggerSync() {
        statusLabel.setText("Synchronisation Bluetooth (SMS_GET_RECENT|20)...");
        statusLabel.setColor(0xFACC15);
        app.getMessageManager().requestRecentSms();
    }

    private void rebuildMessagesList() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    listContainer.deleteAll();
                    Vector msgs = app.getMessageManager().getMessages();

                    countLabel.setText(msgs.size() + " message(s) synchronisé(s)");

                    if (msgs.isEmpty()) {
                        VerticalFieldManager emptyBox = new VerticalFieldManager(Field.FIELD_HCENTER);
                        emptyBox.setPadding(12, 10, 12, 10);
                        DarkLabelField emptyLbl = new DarkLabelField("Aucun SMS dans la boîte", Field.FIELD_HCENTER, 0x64748B);
                        emptyBox.add(emptyLbl);
                        listContainer.add(emptyBox);
                        return;
                    }

                    for (int i = 0; i < msgs.size(); i++) {
                        final SmsItem sms = (SmsItem) msgs.elementAt(i);
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
        String body = "[" + sms.simName + " | " + sms.timestamp + "]\n\n" + sms.body;

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
            app.getUIManager().pushScreen(new ComposeSmsScreen(app, sms.senderNumber, sms.senderName));
        } else if (choice == 2) {
            app.getMessageManager().deleteMessage(sms);
        }
    }

    private void callContactOfSms(SmsItem sms) {
        if (sms != null && sms.senderNumber != null && sms.senderNumber.length() > 0) {
            statusLabel.setText("Appel en cours : " + sms.getDisplayName() + "...");
            statusLabel.setColor(0x22C55E);
            app.getCallManager().initiateOutboundCall(sms.senderNumber, sms.senderName);
        } else {
            Dialog.alert("Numéro de contact introuvable.");
        }
    }

    private SmsItem getFocusedSms() {
        Field focused = getLeafFieldWithFocus();
        if (focused instanceof SmsCardField) {
            return ((SmsCardField) focused).getSms();
        }
        Vector msgs = app.getMessageManager().getMessages();
        if (msgs != null && !msgs.isEmpty()) {
            return (SmsItem) msgs.elementAt(0);
        }
        return null;
    }

    // =========================================================================
    // Interception des touches physiques BlackBerry Curve 9300
    // =========================================================================

    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);

        // 1. Touche Appel (Verte / KEY_SEND) :
        // Si dans la liste des SMS : lancer l'appel direct vers le contact sélectionné !
        if (key == Keypad.KEY_SEND) {
            SmsItem current = getFocusedSms();
            if (current != null) {
                callContactOfSms(current);
                return true;
            } else {
                app.getUIManager().pushScreen(new PhoneDialerScreen(app, app.getCallManager()));
                return true;
            }
        }

        // 2. Touche Fin d'appel (Rouge / KEY_END ou ESCAPE) :
        // Ferme l'écran de la boîte de réception
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
        menu.add(new MenuItem("Rédiger Nouveau SMS", 100, 10) {
            public void run() {
                app.getUIManager().pushScreen(new ComposeSmsScreen(app, "", ""));
            }
        });

        final SmsItem focused = getFocusedSms();
        if (focused != null) {
            menu.add(new MenuItem("Appeler ce contact (Touche Verte)", 100, 20) {
                public void run() { callContactOfSms(focused); }
            });

            menu.add(new MenuItem("Répondre par SMS", 100, 30) {
                public void run() {
                    app.getUIManager().pushScreen(new ComposeSmsScreen(app, focused.senderNumber, focused.senderName));
                }
            });

            menu.add(new MenuItem("Supprimer ce SMS", 100, 40) {
                public void run() {
                    app.getMessageManager().deleteMessage(focused);
                }
            });
        }

        menu.add(new MenuItem("Actualiser (SMS_GET_RECENT|20)", 100, 50) {
            public void run() { triggerSync(); }
        });

        menu.add(new MenuItem("Compositeur d'Appels", 100, 60) {
            public void run() {
                app.getUIManager().pushScreen(new PhoneDialerScreen(app, app.getCallManager()));
            }
        });

        menu.add(new MenuItem("Vider toute la boîte", 100, 70) {
            public void run() {
                if (Dialog.ask(Dialog.D_YES_NO, "Voulez-vous supprimer tous les SMS ?") == Dialog.YES) {
                    app.getMessageManager().clearAllMessages();
                }
            }
        });

        super.makeMenu(menu, instance);
    }

    // =========================================================================
    // Champ visuel personnalisé pour chaque SMS (SmsCardField)
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

        public int getPreferredWidth() {
            return 308;
        }

        public int getPreferredHeight() {
            return 46;
        }

        protected void layout(int width, int height) {
            setExtent(Math.min(width, getPreferredWidth()), getPreferredHeight());
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            boolean focused = isFocus();

            // Background
            if (focused) {
                g.setColor(0x0284C7); // Cyan surbrillance au trackpad
                g.fillRoundRect(0, 0, w, h, 6, 6);
                g.setColor(0x38BDF8);
                g.drawRoundRect(0, 0, w, h, 6, 6);
            } else {
                g.setColor(0x1E293B);
                g.fillRoundRect(0, 0, w, h, 6, 6);
                g.setColor(0x334155);
                g.drawRoundRect(0, 0, w, h, 6, 6);
            }

            // Direction indicator icon / color
            int arrowColor = sms.isOutgoing ? 0x00E5FF : 0x22C55E;
            g.setColor(arrowColor);
            g.fillRoundRect(4, 6, 5, h - 12, 2, 2);

            // Row 1: Sender Name
            g.setColor(Color.WHITE);
            try { g.setFont(Font.getDefault().derive(Font.BOLD, 12)); } catch (Throwable ignored) {}
            String name = sms.getDisplayName();
            if (name.length() > 20) name = name.substring(0, 19) + "..";
            g.drawText(name, 14, 4);

            // Row 1 Right: SIM badge & Time
            try { g.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
            g.setColor(0xFACC15); // Or SIM
            String tag = "[" + sms.simName + "] " + sms.timestamp;
            g.drawText(tag, w - 105, 4);

            // Row 2: Snippet
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
