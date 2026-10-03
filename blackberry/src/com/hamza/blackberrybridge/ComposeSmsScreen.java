package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

/**
 * Écran de rédaction rapide de SMS pour BlackBerry Curve 9300.
 * Tire 100% parti du clavier physique complet AZERTY/QWERTY.
 *
 * Raccourcis physiques :
 * - Touche Verte (KEY_SEND) : Envoi immédiat du SMS
 * - Touche Rouge (KEY_END) : Annulation et fermeture
 * - Touche Menu BlackBerry : Choix SIM 1 / SIM 2, Insérer contact, Effacer
 */
public class ComposeSmsScreen extends MainScreen {
    private SmartBridgeApp app;
    private BasicEditField recipientField;
    private BasicEditField messageField;
    private DarkLabelField charCounterLabel;
    private DarkLabelField simBadgeLabel;
    private DarkButtonField btnSend;
    private DarkButtonField btnToggleSim;

    private int selectedSlot = 0; // 0 = SIM 1, 1 = SIM 2
    private String recipientName = "";

    public ComposeSmsScreen(SmartBridgeApp app, String prefilledNumber, String contactName) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.app = app;
        this.recipientName = (contactName != null) ? contactName : "";

        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));

        VerticalFieldManager root = new VerticalFieldManager(Field.FIELD_HCENTER);
        root.setPadding(4, 6, 4, 6);

        // Header Title
        DarkLabelField title = new DarkLabelField("NOUVEAU SMS", Field.FIELD_HCENTER, 0x00E5FF);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch (Throwable ignored) {}
        root.add(title);

        // SIM Badge
        simBadgeLabel = new DarkLabelField(getSimLabelText(), Field.FIELD_HCENTER, 0xFACC15);
        try { simBadgeLabel.setFont(Font.getDefault().derive(Font.BOLD, 11)); } catch (Throwable ignored) {}
        root.add(simBadgeLabel);

        // Recipient Label & Input
        DarkLabelField toLabel = new DarkLabelField("Destinataire :", Field.FIELD_LEFT, 0x94A3B8);
        try { toLabel.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
        root.add(toLabel);

        VerticalFieldManager recipientWrapper = new VerticalFieldManager();
        recipientWrapper.setBackground(BackgroundFactory.createSolidBackground(0x1E293B));
        recipientWrapper.setPadding(3, 6, 3, 6);

        String initialNum = prefilledNumber != null ? prefilledNumber : "";
        recipientField = new BasicEditField("", initialNum, 35, BasicEditField.FILTER_PHONE);
        try { recipientField.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch (Throwable ignored) {}
        recipientWrapper.add(recipientField);
        root.add(recipientWrapper);

        // Message Label & Input
        DarkLabelField bodyLabel = new DarkLabelField("Message (Clavier physique) :", Field.FIELD_LEFT, 0x94A3B8);
        try { bodyLabel.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
        root.add(bodyLabel);

        VerticalFieldManager messageWrapper = new VerticalFieldManager();
        messageWrapper.setBackground(BackgroundFactory.createSolidBackground(0x1E293B));
        messageWrapper.setPadding(4, 6, 4, 6);

        messageField = new BasicEditField("", "", 500, BasicEditField.NO_NEWLINE) {
            protected boolean keyChar(char ch, int status, int time) {
                boolean res = super.keyChar(ch, status, time);
                updateCounter();
                return res;
            }
        };
        try { messageField.setFont(Font.getDefault().derive(Font.PLAIN, 13)); } catch (Throwable ignored) {}
        messageWrapper.add(messageField);
        root.add(messageWrapper);

        // Character counter
        charCounterLabel = new DarkLabelField("Caractères : 0 / 160 (1 SMS)", Field.FIELD_RIGHT, 0x64748B);
        try { charCounterLabel.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
        root.add(charCounterLabel);

        // Action Buttons Bar
        HorizontalFieldManager btnBar = new HorizontalFieldManager(Field.FIELD_HCENTER);
        btnBar.setPadding(4, 0, 4, 0);

        btnSend = new DarkButtonField("Envoyer (Vert)", 110, 28, DarkButtonField.STYLE_GREEN);
        btnSend.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                doSendSms();
            }
        });

        btnToggleSim = new DarkButtonField("Changer SIM", 95, 28, DarkButtonField.STYLE_CYAN);
        btnToggleSim.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                toggleSim();
            }
        });

        DarkButtonField btnCancel = new DarkButtonField("Annuler", 75, 28, DarkButtonField.STYLE_SLATE);
        btnCancel.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                close();
            }
        });

        btnBar.add(btnSend);
        btnBar.add(btnToggleSim);
        btnBar.add(btnCancel);
        root.add(btnBar);

        DarkLabelField footerHint = new DarkLabelField("Touche Verte: Envoyer | Touche Rouge: Fermer", Field.FIELD_HCENTER, 0x64748B);
        try { footerHint.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
        root.add(footerHint);

        add(root);

        // Focus message field if recipient is prefilled
        if (initialNum.length() > 0) {
            messageField.setFocus();
        } else {
            recipientField.setFocus();
        }
    }

    private String getSimLabelText() {
        String simName = "SIM " + (selectedSlot + 1);
        if (app != null && app.getCallManager() != null) {
            SimCard sim = app.getCallManager().getSim(selectedSlot);
            if (sim != null && sim.getName() != null) {
                simName = sim.getName();
            }
        }
        return "Envoi via : [" + simName + "]";
    }

    private void toggleSim() {
        selectedSlot = (selectedSlot == 0) ? 1 : 0;
        simBadgeLabel.setText(getSimLabelText());
    }

    private void updateCounter() {
        int len = messageField.getText().length();
        int smsCount = (len <= 160) ? 1 : ((len / 153) + 1);
        charCounterLabel.setText("Caractères : " + len + " (" + smsCount + " SMS)");
    }

    private void doSendSms() {
        String recipient = recipientField.getText().trim();
        String text = messageField.getText().trim();

        if (recipient.length() == 0) {
            Dialog.alert("Veuillez saisir un numéro de destinataire.");
            recipientField.setFocus();
            return;
        }

        if (text.length() == 0) {
            Dialog.alert("Veuillez saisir le contenu du message SMS.");
            messageField.setFocus();
            return;
        }

        app.getMessageManager().sendSms(recipient, text, selectedSlot);
        close();
    }

    // =========================================================================
    // Raccourcis physiques BlackBerry
    // =========================================================================

    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);

        // Touche Verte (KEY_SEND) : Envoi direct du SMS rédigé
        if (key == Keypad.KEY_SEND) {
            doSendSms();
            return true;
        }

        // Touche Rouge (KEY_END) : Annuler et fermer
        if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyDown(keycode, time);
    }

    protected void makeMenu(Menu menu, int instance) {
        menu.add(new MenuItem("Envoyer le SMS (Touche Verte)", 100, 10) {
            public void run() { doSendSms(); }
        });

        menu.add(new MenuItem("Utiliser SIM 1", 100, 20) {
            public void run() {
                selectedSlot = 0;
                simBadgeLabel.setText(getSimLabelText());
            }
        });

        menu.add(new MenuItem("Utiliser SIM 2", 100, 30) {
            public void run() {
                selectedSlot = 1;
                simBadgeLabel.setText(getSimLabelText());
            }
        });

        menu.add(new MenuItem("Effacer le texte", 100, 40) {
            public void run() {
                messageField.setText("");
                updateCounter();
            }
        });

        super.makeMenu(menu, instance);
    }
}
