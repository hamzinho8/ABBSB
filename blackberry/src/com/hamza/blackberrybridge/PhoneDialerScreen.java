package com.hamza.blackberrybridge;

import java.util.Vector;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.system.Characters;

/**
 * Compositeur de Numéro et Contrôleur Téléphonique / DTMF / USSD
 * Optimisé pour BlackBerry Curve 9300 (OS 6.0).
 *
 * Exploite 100% des touches physiques :
 * - Touche Verte (KEY_SEND) : Lancement immédiat de l'appel ou du code USSD.
 * - Touche Rouge (KEY_END) : Raccrochage immédiat ou fermeture d'écran.
 * - Clavier physique 0-9, *, # : Envoi DTMF temps réel en communication, ou composition.
 * - Touche Menu BlackBerry : Bascule Double SIM, SMS direct, Journal d'appels.
 */
public class PhoneDialerScreen extends MainScreen {
    private SmartBridgeApp app;
    private CallManager callManager;

    private BasicEditField numberInput;
    private DarkLabelField modeBadge;
    private DarkLabelField statusFeedback;
    private HorizontalFieldManager actionButtonBar;
    private DarkButtonField btnCallSim1;
    private DarkButtonField btnCallSim2;
    private DarkButtonField btnSendUssd;
    private DarkButtonField btnClear;

    private int selectedSlot = 0; // 0 = SIM 1, 1 = SIM 2
    private String lastDialedNumber = "";

    public PhoneDialerScreen(final SmartBridgeApp app, final CallManager callManager) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.app = app;
        this.callManager = callManager;

        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));

        VerticalFieldManager root = new VerticalFieldManager(Field.FIELD_HCENTER);
        root.setPadding(4, 6, 4, 6);

        // Header
        DarkLabelField title = new DarkLabelField("COMPOSITEUR & DTMF", Field.FIELD_HCENTER, 0x00E5FF);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch (Throwable ignored) {}
        root.add(title);

        // Subtitle SIM indicators
        String simInfo = getSimInfoBanner();
        DarkLabelField simLabel = new DarkLabelField(simInfo, Field.FIELD_HCENTER, 0x94A3B8);
        try { simLabel.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
        root.add(simLabel);

        // Champ de saisie stylisé
        VerticalFieldManager inputWrapper = new VerticalFieldManager(Field.FIELD_HCENTER);
        inputWrapper.setBackground(BackgroundFactory.createSolidBackground(0x1E293B));
        inputWrapper.setPadding(4, 8, 4, 8);
        inputWrapper.setMargin(4, 0, 4, 0);

        numberInput = new BasicEditField("", "", 35, BasicEditField.FILTER_PHONE) {
            protected boolean keyChar(char ch, int status, int time) {
                boolean res = super.keyChar(ch, status, time);
                updateDialerState();
                return res;
            }
        };
        try { numberInput.setFont(Font.getDefault().derive(Font.BOLD, 18)); } catch (Throwable ignored) {}
        inputWrapper.add(numberInput);

        modeBadge = new DarkLabelField("Mode : Numéro Téléphonique", Field.FIELD_HCENTER, 0x38BDF8);
        try { modeBadge.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
        inputWrapper.add(modeBadge);

        root.add(inputWrapper);

        // Statut / Feedback (DTMF émis, USSD, etc.)
        statusFeedback = new DarkLabelField("Prêt - Clavier physique actif", Field.FIELD_HCENTER, 0x22C55E);
        try { statusFeedback.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
        root.add(statusFeedback);

        // Barre de boutons d'action
        actionButtonBar = new HorizontalFieldManager(Field.FIELD_HCENTER);
        actionButtonBar.setPadding(2, 0, 4, 0);

        btnCallSim1 = new DarkButtonField("Appel SIM 1", 78, 26, DarkButtonField.STYLE_GREEN);
        btnCallSim1.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                dialWithSim(0);
            }
        });

        btnCallSim2 = new DarkButtonField("Appel SIM 2", 78, 26, DarkButtonField.STYLE_CYAN);
        btnCallSim2.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                dialWithSim(1);
            }
        });

        btnSendUssd = new DarkButtonField("Code USSD", 78, 26, DarkButtonField.STYLE_GOLD);
        btnSendUssd.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                sendCurrentUssd();
            }
        });

        btnClear = new DarkButtonField("Effacer", 64, 26, DarkButtonField.STYLE_SLATE);
        btnClear.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                clearInput();
            }
        });

        actionButtonBar.add(btnCallSim1);
        actionButtonBar.add(btnCallSim2);
        actionButtonBar.add(btnClear);
        root.add(actionButtonBar);

        // Pavé numérique / DTMF interactif
        root.add(createDialerGrid());

        DarkLabelField footerHint = new DarkLabelField("Vert: Appeler | Rouge: Raccrocher | Clavier: 0-9 * #", Field.FIELD_HCENTER, 0x64748B);
        try { footerHint.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
        root.add(footerHint);

        add(root);
    }

    private String getSimInfoBanner() {
        if (callManager != null && callManager.getSimCards().size() >= 2) {
            SimCard s1 = callManager.getSim(0);
            SimCard s2 = callManager.getSim(1);
            return "[SIM 1: " + (s1 != null ? s1.getName() : "SIM 1") + "]  [SIM 2: " + (s2 != null ? s2.getName() : "SIM 2") + "]";
        } else if (callManager != null && callManager.getSimCards().size() == 1) {
            SimCard s1 = callManager.getSim(0);
            return "[SIM: " + (s1 != null ? s1.getName() : "Active") + "]";
        }
        return "[Réseau Double SIM Prêt]";
    }

    private VerticalFieldManager createDialerGrid() {
        VerticalFieldManager grid = new VerticalFieldManager(Field.FIELD_HCENTER);
        grid.setPadding(2, 0, 4, 0);

        String[][] keyLabels = new String[][] {
            { "1", "2 ABC", "3 DEF" },
            { "4 GHI", "5 JKL", "6 MNO" },
            { "7 PQRS", "8 TUV", "9 WXYZ" },
            { "*", "0 +", "#" }
        };

        final char[][] charKeys = new char[][] {
            { '1', '2', '3' },
            { '4', '5', '6' },
            { '7', '8', '9' },
            { '*', '0', '#' }
        };

        for (int row = 0; row < 4; row++) {
            HorizontalFieldManager rowManager = new HorizontalFieldManager(Field.FIELD_HCENTER);
            for (int col = 0; col < 3; col++) {
                final char c = charKeys[row][col];
                String lbl = keyLabels[row][col];

                DarkButtonField keyBtn = new DarkButtonField(lbl, 96, 26, DarkButtonField.STYLE_SLATE);
                keyBtn.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        handleDialerInput(c);
                    }
                });
                rowManager.add(keyBtn);
            }
            grid.add(rowManager);
        }

        return grid;
    }

    private void handleDialerInput(char ch) {
        if (callManager != null && callManager.isCallInProgress()) {
            // Envoi DTMF immédiat pendant la communication
            callManager.sendDtmf(ch);
            setFeedback("DTMF émis : '" + ch + "'", 0x38BDF8);
        } else {
            // Ajout du caractère au compositeur
            String current = numberInput.getText();
            numberInput.setText(current + ch);
            updateDialerState();
        }
    }

    private void updateDialerState() {
        String num = numberInput.getText().trim();
        boolean isUssd = isUssdCode(num);

        if (isUssd) {
            modeBadge.setText("Mode : Code USSD (" + num + ")");
            modeBadge.setColor(0xF59E0B);
            setFeedback("Code USSD prêt. Appuyez sur la Touche Verte pour envoyer.", 0xF59E0B);
        } else if (num.length() > 0) {
            modeBadge.setText("Mode : Appel (" + num + ")");
            modeBadge.setColor(0x22C55E);
            setFeedback("Numéro composé. Touche Verte pour appeler.", 0x22C55E);
        } else {
            modeBadge.setText("Mode : Numéro Téléphonique");
            modeBadge.setColor(0x38BDF8);
            setFeedback("Prêt - Clavier physique actif", 0x22C55E);
        }
    }

    private boolean isUssdCode(String s) {
        if (s == null) return false;
        String trimmed = s.trim();
        return trimmed.startsWith("*") && trimmed.endsWith("#");
    }

    private void dialWithSim(int slot) {
        String num = numberInput.getText().trim();
        if (num.length() == 0) {
            if (lastDialedNumber.length() > 0) {
                numberInput.setText(lastDialedNumber);
                updateDialerState();
                return;
            }
            Dialog.alert("Veuillez saisir un numéro ou un code USSD.");
            return;
        }

        lastDialedNumber = num;

        if (isUssdCode(num)) {
            callManager.sendUssd(num, slot);
            setFeedback("Requête USSD envoyée sur SIM " + (slot + 1) + "...", 0xF59E0B);
        } else {
            String simName = "SIM " + (slot + 1);
            if (callManager != null) {
                SimCard sim = callManager.getSim(slot);
                if (sim != null && sim.getName() != null) {
                    simName = sim.getName();
                }
            }
            setFeedback("Lancement de l'appel via " + simName + "...", 0x22C55E);
            callManager.initiateOutboundCall(num, "");
        }
    }

    private void sendCurrentUssd() {
        String num = numberInput.getText().trim();
        if (!isUssdCode(num)) {
            Dialog.alert("Le format doit commencer par '*' et se terminer par '#'. Exemple: *100#");
            return;
        }
        callManager.sendUssd(num, selectedSlot);
    }

    private void clearInput() {
        numberInput.setText("");
        updateDialerState();
    }

    private void setFeedback(final String msg, final int color) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    statusFeedback.setText(msg);
                    statusFeedback.setColor(color);
                } catch (Throwable ignored) {}
            }
        });
    }

    // =========================================================================
    // Interception des touches physiques BlackBerry Curve 9300
    // =========================================================================

    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);

        // 1. Touche Appel (Verte / KEY_SEND)
        if (key == Keypad.KEY_SEND) {
            String num = numberInput.getText().trim();
            if (num.length() > 0) {
                if (isUssdCode(num)) {
                    sendCurrentUssd();
                } else {
                    dialWithSim(selectedSlot);
                }
                return true;
            } else if (lastDialedNumber.length() > 0) {
                numberInput.setText(lastDialedNumber);
                updateDialerState();
                return true;
            }
        }

        // 2. Touche Fin d'appel (Rouge / KEY_END)
        if (key == Keypad.KEY_END) {
            if (callManager != null && callManager.isCallInProgress()) {
                callManager.endCurrentCall();
                setFeedback("Appel raccroché.", 0xEF4444);
                return true;
            }
            if (numberInput.getText().length() > 0) {
                clearInput();
                return true;
            }
            close();
            return true;
        }

        // 3. Touche Échap (ESCAPE)
        if (key == Keypad.KEY_ESCAPE) {
            if (numberInput.getText().length() > 0) {
                clearInput();
                return true;
            }
            close();
            return true;
        }

        return super.keyDown(keycode, time);
    }

    protected boolean keyChar(char ch, int status, int time) {
        // En cours de communication, toutes les touches chiffres / symboles envoient du DTMF
        if (callManager != null && callManager.isCallInProgress()) {
            if ((ch >= '0' && ch <= '9') || ch == '*' || ch == '#') {
                handleDialerInput(ch);
                return true;
            }
        }

        // Clavier physique hors communication : composition numérique
        if ((ch >= '0' && ch <= '9') || ch == '*' || ch == '#' || ch == '+') {
            handleDialerInput(ch);
            return true;
        }

        if (ch == '\b' || ch == 8) { // Backspace
            String cur = numberInput.getText();
            if (cur.length() > 0) {
                numberInput.setText(cur.substring(0, cur.length() - 1));
                updateDialerState();
            }
            return true;
        }

        return super.keyChar(ch, status, time);
    }

    // =========================================================================
    // Menu BlackBerry OS 6.0
    // =========================================================================

    protected void makeMenu(Menu menu, int instance) {
        menu.add(new MenuItem("Appeler avec SIM 1", 100, 10) {
            public void run() { dialWithSim(0); }
        });

        menu.add(new MenuItem("Appeler avec SIM 2", 100, 20) {
            public void run() { dialWithSim(1); }
        });

        menu.add(new MenuItem("Envoyer Code USSD", 100, 30) {
            public void run() { sendCurrentUssd(); }
        });

        menu.add(new MenuItem("Nouveau SMS vers ce numéro", 100, 40) {
            public void run() {
                String num = numberInput.getText().trim();
                app.getUIManager().pushScreen(new ComposeSmsScreen(app, num, ""));
            }
        });

        menu.add(new MenuItem("Boîte de Réception SMS", 100, 50) {
            public void run() {
                app.getUIManager().pushScreen(new SmsHubScreen(app));
            }
        });

        menu.add(new MenuItem("Journal des Appels", 100, 60) {
            public void run() {
                app.getUIManager().openCallHistory();
            }
        });

        menu.add(new MenuItem("Carnet de Contacts", 100, 70) {
            public void run() {
                app.getUIManager().openContacts();
            }
        });

        menu.add(new MenuItem("Effacer la saisie", 100, 80) {
            public void run() { clearInput(); }
        });

        super.makeMenu(menu, instance);
    }
}
