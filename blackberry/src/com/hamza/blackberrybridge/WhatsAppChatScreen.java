package com.hamza.blackberrybridge;

import java.util.Vector;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.system.Characters;

/**
 * Écran de messagerie instantanée WhatsApp pour BlackBerry Curve 9300.
 * Hérite de MainScreen avec scroll fluide à la trackpad optique
 * et champ de réponse rapide tirant 100% parti du clavier physique complet.
 */
public class WhatsAppChatScreen extends MainScreen implements WhatsAppManager.WhatsAppListener {
    private WhatsAppManager whatsAppManager;
    private VerticalFieldManager messagesContainer;
    private BasicEditField inputField;
    private DarkButtonField btnSend;
    private DarkLabelField contactSubtitle;

    private String currentNotifId = "wa_general";
    private String currentContactName = "WhatsApp";

    public WhatsAppChatScreen(final WhatsAppManager manager) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.whatsAppManager = manager;

        // Fond sombre WhatsApp Web / Mobile (#0B141A)
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(0x0B141A));

        VerticalFieldManager root = new VerticalFieldManager(Field.FIELD_HCENTER);
        root.setPadding(2, 6, 2, 6);

        // 1. En-tête WhatsApp
        VerticalFieldManager header = new VerticalFieldManager(Field.FIELD_HCENTER);
        header.setBackground(BackgroundFactory.createSolidBackground(0x128C7E));
        header.setPadding(3, 8, 3, 8);

        DarkLabelField title = new DarkLabelField("WHATSAPP MESSENGER", Field.FIELD_HCENTER, Color.WHITE);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 13)); } catch (Throwable ignored) {}
        header.add(title);

        contactSubtitle = new DarkLabelField("En ligne via Android | Clavier physique actif", Field.FIELD_HCENTER, 0xD1FAE5);
        try { contactSubtitle.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
        header.add(contactSubtitle);
        root.add(header);

        // 2. Conteneur des bulles de messages
        messagesContainer = new VerticalFieldManager(Field.FIELD_HCENTER);
        messagesContainer.setPadding(4, 0, 4, 0);
        root.add(messagesContainer);

        // 3. Barre de réponse fixe en bas
        HorizontalFieldManager inputBar = new HorizontalFieldManager(Field.FIELD_HCENTER);
        inputBar.setBackground(BackgroundFactory.createSolidBackground(0x1F2C34));
        inputBar.setPadding(4, 6, 4, 6);

        VerticalFieldManager inputWrapper = new VerticalFieldManager();
        inputWrapper.setBackground(BackgroundFactory.createSolidBackground(0x2A3942));
        inputWrapper.setPadding(3, 6, 3, 6);

        inputField = new BasicEditField("", "", 400, BasicEditField.NO_NEWLINE) {
            protected boolean keyChar(char ch, int status, int time) {
                // Touche Entrée : Envoi immédiat du message rédigé
                if (ch == Characters.ENTER || ch == '\n') {
                    sendReplyMessage();
                    return true;
                }
                return super.keyChar(ch, status, time);
            }
        };
        try { inputField.setFont(Font.getDefault().derive(Font.PLAIN, 12)); } catch (Throwable ignored) {}
        inputWrapper.add(inputField);

        btnSend = new DarkButtonField("Envoyer", 65, 26, DarkButtonField.STYLE_GREEN);
        btnSend.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                sendReplyMessage();
            }
        });

        inputBar.add(inputWrapper);
        inputBar.add(btnSend);
        root.add(inputBar);

        // Ligne d'aide
        DarkLabelField hint = new DarkLabelField("Entrée ou Clic: Envoyer | Touche Verte: Appel WA | Rouge: Quitter", Field.FIELD_HCENTER, 0x8696A0);
        try { hint.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch (Throwable ignored) {}
        root.add(hint);

        add(root);

        // Écouteur WhatsApp en temps réel
        if (whatsAppManager != null) {
            whatsAppManager.addListener(this);
        }

        rebuildChatList();
        inputField.setFocus();
    }

    public boolean onClose() {
        if (whatsAppManager != null) {
            whatsAppManager.removeListener(this);
        }
        return super.onClose();
    }

    public void onMessagesUpdated() {
        rebuildChatList();
    }

    public void onNewMessage(WhatsAppMessage msg) {
        if (msg != null) {
            currentNotifId = msg.notifId;
            currentContactName = msg.senderName;
        }
        rebuildChatList();
    }

    public void onCallEnded(String callId) {
        // No-op
    }

    private void rebuildChatList() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    messagesContainer.deleteAll();
                    Vector msgs = (whatsAppManager != null) ? whatsAppManager.getMessages() : new Vector();

                    if (msgs.isEmpty()) {
                        DarkLabelField emptyLbl = new DarkLabelField("Aucun message WhatsApp", Field.FIELD_HCENTER, 0x8696A0);
                        messagesContainer.add(emptyLbl);
                        return;
                    }

                    for (int i = 0; i < msgs.size(); i++) {
                        WhatsAppMessage m = (WhatsAppMessage) msgs.elementAt(i);
                        if (!m.isOutgoing) {
                            currentNotifId = m.notifId;
                            currentContactName = m.senderName;
                        }
                        messagesContainer.add(new MessageBubbleField(m));
                    }

                    contactSubtitle.setText("Discussion : " + currentContactName + " | Clavier actif");
                } catch (Throwable t) {
                    System.out.println("[BB ERROR] " + t.getMessage());
                }
            }
        });
    }

    private void sendReplyMessage() {
        String text = inputField.getText().trim();
        if (text.length() == 0) return;

        if (whatsAppManager != null) {
            whatsAppManager.sendReply(currentNotifId, text);
        }

        inputField.setText("");
        rebuildChatList();
        inputField.setFocus();
    }

    private void promptNewChat() {
        final EditField phoneField = new EditField("N° Téléphone : ", "+212", 20, BasicEditField.FILTER_PHONE);
        final EditField msgField = new EditField("Message : ", "", 200, BasicEditField.NO_NEWLINE);

        PopupScreen popup = new PopupScreen(new VerticalFieldManager());
        popup.add(new LabelField("Nouveau Chat WhatsApp"));
        popup.add(phoneField);
        popup.add(msgField);

        ButtonField btnOk = new ButtonField("Démarrer Chat", ButtonField.CONSUME_CLICK);
        btnOk.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                String p = phoneField.getText().trim();
                String m = msgField.getText().trim();
                if (p.length() > 0 && whatsAppManager != null) {
                    whatsAppManager.startNewChat(p, m);
                }
                UiApplication.getUiApplication().popScreen(UiApplication.getUiApplication().getActiveScreen());
            }
        });
        popup.add(btnOk);
        UiApplication.getUiApplication().pushScreen(popup);
    }

    // =========================================================================
    // Raccourcis clavier physique BlackBerry Curve 9300
    // =========================================================================

    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);

        // Touche Verte (KEY_SEND) : Si message saisi -> Envoyer. Sinon -> Lancer appel WhatsApp
        if (key == Keypad.KEY_SEND) {
            if (inputField.getText().trim().length() > 0) {
                sendReplyMessage();
                return true;
            } else if (currentContactName != null && currentContactName.length() > 0) {
                // Proposer ou lancer l'appel WhatsApp
                whatsAppManager.handleIncomingCall("call_" + System.currentTimeMillis(), currentContactName);
                return true;
            }
        }

        // Touche Rouge (KEY_END) ou Échap : Ferme l'écran de chat
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
        menu.add(new MenuItem("Envoyer la réponse", 100, 10) {
            public void run() { sendReplyMessage(); }
        });

        menu.add(new MenuItem("Appel WhatsApp (Touche Verte)", 100, 20) {
            public void run() {
                whatsAppManager.handleIncomingCall("out_" + System.currentTimeMillis(), currentContactName);
            }
        });

        menu.add(new MenuItem("Nouveau Chat WhatsApp", 100, 30) {
            public void run() { promptNewChat(); }
        });

        menu.add(new MenuItem("Effacer la discussion", 100, 40) {
            public void run() {
                if (whatsAppManager != null) {
                    whatsAppManager.clearConversation();
                }
            }
        });

        super.makeMenu(menu, instance);
    }

    // =========================================================================
    // Composant de Bulle de Dialogue WhatsApp (Vert émeraude / Gris foncé)
    // =========================================================================

    private static class MessageBubbleField extends Field {
        private WhatsAppMessage msg;

        public MessageBubbleField(WhatsAppMessage msg) {
            super(Field.NON_FOCUSABLE);
            this.msg = msg;
        }

        public int getPreferredWidth() { return 308; }
        public int getPreferredHeight() { return 38; }

        protected void layout(int width, int height) {
            setExtent(308, 38);
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            if (msg.isOutgoing) {
                // Bulle sortante : Vert foncé WhatsApp (#005C4B) alignée à droite
                int bubbleW = Math.min(240, w - 40);
                int bubbleX = w - bubbleW - 4;

                g.setColor(0x005C4B);
                g.fillRoundRect(bubbleX, 2, bubbleW, h - 4, 8, 8);
                g.setColor(0x128C7E);
                g.drawRoundRect(bubbleX, 2, bubbleW, h - 4, 8, 8);

                // Texte blanc
                g.setColor(Color.WHITE);
                try { g.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
                String text = msg.body;
                if (text.length() > 32) text = text.substring(0, 31) + "..";
                g.drawText(text, bubbleX + 8, 6);

                // Heure et statut "✓✓"
                g.setColor(0x34B7F1); // Bleu double check WhatsApp
                try { g.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch (Throwable ignored) {}
                g.drawText(msg.timestamp + " vv", bubbleX + bubbleW - 48, h - 14);

            } else {
                // Bulle entrante : Gris anthracite (#202C33) alignée à gauche
                int bubbleW = Math.min(240, w - 40);
                int bubbleX = 4;

                g.setColor(0x202C33);
                g.fillRoundRect(bubbleX, 2, bubbleW, h - 4, 8, 8);
                g.setColor(0x2A3942);
                g.drawRoundRect(bubbleX, 2, bubbleW, h - 4, 8, 8);

                // Nom de l'expéditeur en vert émeraude
                g.setColor(0x25D366);
                try { g.setFont(Font.getDefault().derive(Font.BOLD, 10)); } catch (Throwable ignored) {}
                g.drawText(msg.senderName, bubbleX + 8, 4);

                // Texte du message
                g.setColor(0xE9EDEF);
                try { g.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
                String text = msg.body;
                if (text.length() > 32) text = text.substring(0, 31) + "..";
                g.drawText(text, bubbleX + 8, 18);

                // Heure
                g.setColor(0x8696A0);
                try { g.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch (Throwable ignored) {}
                g.drawText(msg.timestamp, bubbleX + bubbleW - 36, h - 14);
            }
        }
    }
}
