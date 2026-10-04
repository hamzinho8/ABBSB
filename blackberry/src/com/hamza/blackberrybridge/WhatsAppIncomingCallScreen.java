package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

/**
 * Écran plein écran d'appel WhatsApp pour BlackBerry Curve 9300.
 * Hérite de FullScreen pour une immersion complète 320x240.
 *
 * Raccourcis physiques :
 * - Touche Verte (KEY_SEND) : Décrocher immédiatement l'appel WhatsApp.
 * - Touche Rouge (KEY_END) : Rejeter ou raccrocher immédiatement l'appel WhatsApp.
 * - Clavier / Volume : Ajustement du volume d'écoute.
 */
public class WhatsAppIncomingCallScreen extends FullScreen {
    private WhatsAppManager whatsAppManager;
    private String callId;
    private String callerName;
    private boolean isRinging = true;
    private boolean isCallConnected = false;
    private boolean isTerminated = false;

    private DarkLabelField headerLabel;
    private DarkLabelField callerNameLabel;
    private DarkLabelField statusLabel;
    private DarkLabelField timerLabel;
    private DarkLabelField footerGreen;
    private DarkLabelField footerRed;

    private Timer callTimer;
    private int callDurationSeconds = 0;

    public WhatsAppIncomingCallScreen(WhatsAppManager manager, String callId, String callerName) {
        super(FullScreen.VERTICAL_SCROLL);
        this.whatsAppManager = manager;
        this.callId = callId;
        this.callerName = (callerName != null && callerName.length() > 0) ? callerName : "Appel WhatsApp";

        // Fond sombre WhatsApp (#0B141A)
        try {
            setBackground(BackgroundFactory.createSolidBackground(0x0B141A));
        } catch (Throwable ignored) {}

        VerticalFieldManager root = new VerticalFieldManager(Field.FIELD_HCENTER);
        root.setBackground(BackgroundFactory.createSolidBackground(0x0B141A));
        root.setPadding(6, 10, 6, 10);

        // 1. Bandeau supérieur WhatsApp
        HorizontalFieldManager topBar = new HorizontalFieldManager(Field.FIELD_HCENTER);
        topBar.setPadding(2, 6, 4, 6);
        headerLabel = new DarkLabelField("APPEL VOCAL WHATSAPP", Field.FIELD_HCENTER, 0x25D366);
        try { headerLabel.setFont(Font.getDefault().derive(Font.BOLD, 12)); } catch (Throwable ignored) {}
        topBar.add(headerLabel);
        root.add(topBar);

        // 2. Logo / Badge WhatsApp stylisé
        root.add(new WhatsAppLogoField());

        // 3. Nom de l'appelant en grand
        callerNameLabel = new DarkLabelField(this.callerName, Field.FIELD_HCENTER, Color.WHITE);
        try { 
            callerNameLabel.setFont(Font.getDefault().derive(Font.BOLD, 20)); 
        } catch (Throwable t) {
            try { callerNameLabel.setFont(Font.getDefault().derive(Font.BOLD, 16)); } catch (Throwable ignored) {}
        }
        root.add(callerNameLabel);

        // 4. Statut d'appel
        statusLabel = new DarkLabelField("Appel entrant via Android...", Field.FIELD_HCENTER, 0x8696A0);
        try { statusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 12)); } catch (Throwable ignored) {}
        root.add(statusLabel);

        // 5. Chronomètre de durée (vide initialement tant que pas connecté)
        timerLabel = new DarkLabelField("", Field.FIELD_HCENTER, 0x25D366);
        try { timerLabel.setFont(Font.getDefault().derive(Font.BOLD, 18)); } catch (Throwable ignored) {}
        root.add(timerLabel);

        // 6. Pied d'écran interactif (Guide des touches physiques)
        HorizontalFieldManager footer = new HorizontalFieldManager(Field.FIELD_HCENTER);
        footer.setPadding(12, 0, 4, 0);

        footerGreen = new DarkLabelField("[ Touche Verte ] Décrocher", Field.FIELD_LEFT, 0x25D366);
        try { footerGreen.setFont(Font.getDefault().derive(Font.BOLD, 12)); } catch (Throwable ignored) {}

        footerRed = new DarkLabelField("[ Touche Rouge ] Rejeter", Field.FIELD_RIGHT, 0xEF4444);
        try { footerRed.setFont(Font.getDefault().derive(Font.BOLD, 12)); } catch (Throwable ignored) {}

        footer.add(footerGreen);
        footer.add(new DarkLabelField("   |   ", Field.FIELD_HCENTER, 0x475569));
        footer.add(footerRed);
        root.add(footer);

        add(root);
    }

    /**
     * Décroche l'appel WhatsApp et bascule l'interface en communication active.
     */
    public void answer() {
        if (!isRinging || isTerminated) return;
        isRinging = false;
        isCallConnected = true;

        if (whatsAppManager != null) {
            whatsAppManager.answerCall(callId);
        }

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    headerLabel.setText("COMMUNICATION WHATSAPP");
                    headerLabel.setColor(0x00E5FF);
                    statusLabel.setText("Audio vocal connecté (SPP 16 kHz)");
                    statusLabel.setColor(0x38BDF8);

                    timerLabel.setText("00:00");
                    footerGreen.setText("");
                    footerRed.setText("[ Touche Rouge ] Raccrocher");
                    footerRed.setColor(0xEF4444);
                } catch (Throwable ignored) {}
            }
        });

        startCallDurationTimer();
    }

    /**
     * Rejette ou raccroche l'appel WhatsApp.
     */
    public void hangup() {
        if (isTerminated) return;
        isTerminated = true;
        stopCallDurationTimer();

        if (whatsAppManager != null) {
            whatsAppManager.rejectCall(callId);
        }

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    statusLabel.setText(isCallConnected ? "Appel WhatsApp terminé" : "Appel rejeté");
                    statusLabel.setColor(0xEF4444);
                } catch (Throwable ignored) {}
            }
        });

        closeAfterDelay(800);
    }

    public void onCallTerminated() {
        if (isTerminated) return;
        isTerminated = true;
        stopCallDurationTimer();

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    statusLabel.setText("Appel WhatsApp terminé");
                    statusLabel.setColor(0x94A3B8);
                } catch (Throwable ignored) {}
            }
        });

        closeAfterDelay(1000);
    }

    private void startCallDurationTimer() {
        stopCallDurationTimer();
        callDurationSeconds = 0;
        callTimer = new Timer();
        callTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                callDurationSeconds++;
                final int m = callDurationSeconds / 60;
                final int s = callDurationSeconds % 60;
                final String timeStr = (m < 10 ? "0" + m : "" + m) + ":" + (s < 10 ? "0" + s : "" + s);

                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        try {
                            if (timerLabel != null) {
                                timerLabel.setText(timeStr);
                            }
                        } catch (Throwable ignored) {}
                    }
                });
            }
        }, 1000, 1000);
    }

    private void stopCallDurationTimer() {
        if (callTimer != null) {
            callTimer.cancel();
            callTimer = null;
        }
    }

    private void closeAfterDelay(int delayMs) {
        new Timer().schedule(new TimerTask() {
            public void run() {
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        try { close(); } catch (Throwable ignored) {}
                    }
                });
            }
        }, delayMs);
    }

    public boolean onClose() {
        stopCallDurationTimer();
        HardwareManager.stopWhatsAppCallAlert();
        return super.onClose();
    }

    // =========================================================================
    // Interception des touches physiques BlackBerry Curve 9300
    // =========================================================================

    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);

        // 1. Touche Verte (KEY_SEND) : Décroche si l'appel sonne
        if (key == Keypad.KEY_SEND) {
            if (isRinging) {
                answer();
                return true;
            }
            return true;
        }

        // 2. Touche Rouge (KEY_END) ou Échap : Rejeter ou Raccrocher
        if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) {
            hangup();
            return true;
        }

        // 3. Touches de volume physique
        if (key == Keypad.KEY_VOLUME_UP) {
            if (whatsAppManager != null && whatsAppManager.getApp() != null) {
                whatsAppManager.getApp().getCallManager().volumeUp();
            }
            return true;
        }
        if (key == Keypad.KEY_VOLUME_DOWN) {
            if (whatsAppManager != null && whatsAppManager.getApp() != null) {
                whatsAppManager.getApp().getCallManager().volumeDown();
            }
            return true;
        }

        return super.keyDown(keycode, time);
    }

    // =========================================================================
    // Champ graphique pour le Logo WhatsApp
    // =========================================================================

    private static class WhatsAppLogoField extends Field {
        public WhatsAppLogoField() {
            super(Field.NON_FOCUSABLE);
        }

        public int getPreferredWidth() { return 52; }
        public int getPreferredHeight() { return 52; }

        protected void layout(int width, int height) {
            setExtent(52, 52);
        }

        protected void paint(Graphics g) {
            g.setColor(0x25D366);
            g.fillRoundRect(2, 2, 48, 48, 48, 48);

            g.setColor(Color.WHITE);
            g.drawRoundRect(4, 4, 44, 44, 44, 44);

            try { g.setFont(Font.getDefault().derive(Font.BOLD, 16)); } catch (Throwable ignored) {}
            g.drawText("WA", 14, 15);
        }
    }
}
