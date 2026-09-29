package com.hamza.blackberrybridge;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.ui.Keypad;

/**
 * Écran d'accueil principal BBSmartBridge pour BlackBerry Curve 9300.
 * Design ergonomique haute fidélité avec boutons Appels (et Journal), Contacts et Média restaurés.
 */
public class SmartBridgeScreen extends MainScreen {
    private TopStatusBarField statusBar;
    private DarkLabelField clockLabel;
    private DarkLabelField dateLabel;
    private DarkLabelField simLabel;
    private DarkLabelField audioLabel;
    
    // Boutons de la grille 3x2
    private DarkButtonField btnCalls;
    private DarkButtonField btnContacts;
    private DarkButtonField btnMedia;
    private DarkButtonField btnMessages;
    private DarkButtonField btnNotifs;
    private DarkButtonField btnAudio;
    
    private SmartBridgeApp app;
    private Timer uiTimer;

    // Jours et mois en français
    private static final String[] DAYS_FR = {
        "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"
    };
    private static final String[] MONTHS_FR = {
        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    public SmartBridgeScreen(final SmartBridgeApp app) {
        super(MainScreen.NO_VERTICAL_SCROLL | MainScreen.NO_HORIZONTAL_SCROLL);
        this.app = app;
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(0x0A0E14));
        
        // 1. Barre d'état supérieure (Hauteur 20px, 320px)
        statusBar = new TopStatusBarField();
        add(statusBar);
        
        // 2. Zone d'horloge et de statut central
        VerticalFieldManager centerArea = new VerticalFieldManager(Field.FIELD_HCENTER);
        centerArea.setPadding(3, 4, 3, 4);
        
        // Horloge numérique moderne
        clockLabel = new DarkLabelField("14:32", Field.FIELD_HCENTER, Color.WHITE);
        try {
            clockLabel.setFont(Font.getDefault().derive(Font.BOLD, 28));
        } catch (Throwable e) {}
        centerArea.add(clockLabel);
        
        // Date en français
        dateLabel = new DarkLabelField("Mardi 24 Septembre 2026", Field.FIELD_HCENTER, 0x94A3B8);
        try {
            dateLabel.setFont(Font.getDefault().derive(Font.PLAIN, 11));
        } catch (Throwable e) {}
        centerArea.add(dateLabel);
        
        // Statut Double SIM
        simLabel = new DarkLabelField("Double SIM Active : [inwi | Orange]", Field.FIELD_HCENTER, 0x38BDF8);
        try {
            simLabel.setFont(Font.getDefault().derive(Font.BOLD, 10));
        } catch (Throwable e) {}
        centerArea.add(simLabel);

        // Audio mains-libres en Vert éclatant
        audioLabel = new DarkLabelField("Audio Mains-Libres Prêt (SCO)", Field.FIELD_HCENTER, 0x4ADE80);
        try {
            audioLabel.setFont(Font.getDefault().derive(Font.PLAIN, 10));
        } catch (Throwable e) {}
        centerArea.add(audioLabel);
        
        add(centerArea);
        
        // 3. Grille 3x2 de boutons modernes avec Appels, Contacts et Média
        VerticalFieldManager gridContainer = new VerticalFieldManager(Field.FIELD_HCENTER);
        gridContainer.setPadding(3, 2, 3, 2);
        
        int btnW = 98;
        int btnH = 27;
        
        // Ligne 1 : Appels (Cyan) | Contacts (Cyan) | Média (Slate)
        HorizontalFieldManager row1 = new HorizontalFieldManager(Field.FIELD_HCENTER);
        row1.setPadding(0, 0, 3, 0);
        
        btnCalls = new DarkButtonField("Appels", btnW, btnH, DarkButtonField.STYLE_CYAN);
        btnCalls.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                app.getUIManager().openDialer();
            }
        });
        
        btnContacts = new DarkButtonField("Contacts", btnW, btnH, DarkButtonField.STYLE_CYAN);
        btnContacts.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                app.getUIManager().openContacts();
            }
        });
        
        btnMedia = new DarkButtonField("Média", btnW, btnH, DarkButtonField.STYLE_SLATE);
        btnMedia.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                app.getUIManager().openMedia();
            }
        });
        
        row1.add(btnCalls);
        row1.add(btnContacts);
        row1.add(btnMedia);
        gridContainer.add(row1);
        
        // Ligne 2 : Messages (Slate) | Notifs (Slate) | Audio Stream (Gold)
        HorizontalFieldManager row2 = new HorizontalFieldManager(Field.FIELD_HCENTER);
        row2.setPadding(0, 0, 2, 0);
        
        btnMessages = new DarkButtonField("Messages", btnW, btnH, DarkButtonField.STYLE_SLATE);
        btnMessages.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                app.getConnectionManager().sendData("OPEN_APP|Messages\n");
            }
        });

        btnNotifs = new DarkButtonField("Notifs (2)", btnW, btnH, DarkButtonField.STYLE_SLATE);
        btnNotifs.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                app.getUIManager().openNotificationList();
            }
        });
        
        btnAudio = new DarkButtonField("Audio Stream", btnW, btnH, DarkButtonField.STYLE_GOLD);
        btnAudio.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                toggleAudioStreaming();
            }
        });
        
        row2.add(btnMessages);
        row2.add(btnNotifs);
        row2.add(btnAudio);
        gridContainer.add(row2);
        
        add(gridContainer);
        
        // 4. Ligne d'aide basse pour touches physiques Curve 9300
        DarkLabelField hintBottom = new DarkLabelField(
            "Touche Verte: Appels & Journal | Menu: Options",
            Field.FIELD_HCENTER,
            0x64748B
        );
        try {
            hintBottom.setFont(Font.getDefault().derive(Font.PLAIN, 10));
        } catch (Throwable e) {}
        add(hintBottom);
        
        updateTimeAndBBBattery();
        
        // Rafraîchissement régulier de l'heure et de la batterie
        uiTimer = new Timer();
        uiTimer.schedule(new TimerTask() {
            public void run() {
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        updateTimeAndBBBattery();
                    }
                });
            }
        }, 5000, 5000);
    }
    
    private void toggleAudioStreaming() {
        if (com.hamza.blackberrybridge.audio.BlackBerryAudioStreamer.getInstance().isRunning()) {
            com.hamza.blackberrybridge.audio.BlackBerryAudioStreamer.getInstance().stopAudio();
            app.getConnectionManager().sendData("AUDIO_PLAYBACK_STOP\n");
            btnAudio.setText("Audio Stream");
        } else {
            com.hamza.blackberrybridge.audio.BlackBerryAudioStreamer.getInstance().startAudio();
            app.getConnectionManager().sendData("AUDIO_PLAYBACK_START\n");
            btnAudio.setText("Couper Audio");
        }
    }

    private void updateTimeAndBBBattery() {
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        String time = (h < 10 ? "0" + h : "" + h) + ":" + (m < 10 ? "0" + m : "" + m);
        clockLabel.setText(time);
        
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (dayOfWeek < 0 || dayOfWeek >= DAYS_FR.length) dayOfWeek = 0;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        if (month < 0 || month >= MONTHS_FR.length) month = 0;
        int year = cal.get(Calendar.YEAR);
        dateLabel.setText(DAYS_FR[dayOfWeek] + " " + day + " " + MONTHS_FR[month] + " " + year);
        
        int bbBat = BatteryManager.getBatteryLevel();
        if (statusBar != null) {
            statusBar.setBatteryLevel(bbBat);
        }

        if (btnAudio != null) {
            boolean isRunning = com.hamza.blackberrybridge.audio.BlackBerryAudioStreamer.getInstance().isRunning();
            btnAudio.setText(isRunning ? "Couper Audio" : "Audio Stream");
        }
    }
    
    public void updateConnectionStatus(String status) {
        if (statusBar != null) {
            boolean connected = "CONNECTED".equalsIgnoreCase(status);
            statusBar.setBluetoothConnected(connected);
        }
    }
    
    public void updateNetworkTelemetry(String operator, String netType, int signalBars, String status) {
        if (statusBar != null) {
            String bars = "||||";
            if (signalBars == 3) bars = "|||.";
            else if (signalBars == 2) bars = "||..";
            else if (signalBars == 1) bars = "|...";
            else if (signalBars <= 0) bars = "....";
            
            String op = (operator != null && operator.length() > 0) ? operator : "inwi";
            String type = (netType != null && netType.length() > 0) ? netType : "4G";
            statusBar.setNetworkInfo("[" + type + "] " + op + " - Signal: [" + bars + "]");
        }
    }

    public void updateNotificationCount(int count) {
        if (btnNotifs != null) {
            btnNotifs.setText("Notifs (" + count + ")");
        }
    }
    
    public void updateBattery(String level) {
        try {
            int val = Integer.parseInt(level.trim());
            if (statusBar != null) {
                statusBar.setBatteryLevel(val);
            }
        } catch (Throwable e) {}
    }
    
    public void updateWeather(String temp, String unit, String cond, String city) {
    }
    
    public void addLog(String log) {
    }

    private void showHfpInfoDialog() {
        net.rim.device.api.ui.component.Dialog.inform(
            "--- PROFIL MAINS-LIBRES HFP ---\n\n" +
            "SDP Service UUID : 0x111E\n" +
            "Routage SCO : Actif\n\n" +
            "Le son de l'appel et le micro du Curve 9300 fonctionnent en direct avec le smartphone Android sans fil !"
        );
    }
    
    public boolean onClose() {
        if (uiTimer != null) {
            uiTimer.cancel();
            uiTimer = null;
        }
        return super.onClose();
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_SEND) {
            app.getUIManager().openDialer();
            return true;
        }
        return super.keyDown(keycode, time);
    }
    
    protected void makeMenu(net.rim.device.api.ui.component.Menu menu, int instance) {
        super.makeMenu(menu, instance);
        
        menu.add(new net.rim.device.api.ui.MenuItem("Appels & Journal", 100, 5) {
            public void run() {
                app.getUIManager().openDialer();
            }
        });
        
        menu.add(new net.rim.device.api.ui.MenuItem("Contacts VIP", 100, 6) {
            public void run() {
                app.getUIManager().openContacts();
            }
        });

        menu.add(new net.rim.device.api.ui.MenuItem("Lecteur Média", 100, 7) {
            public void run() {
                app.getUIManager().openMedia();
            }
        });

        final boolean isStreaming = com.hamza.blackberrybridge.audio.BlackBerryAudioStreamer.getInstance().isRunning();
        menu.add(new net.rim.device.api.ui.MenuItem(isStreaming ? "Couper Audio Smartphone" : "Écouter Audio Smartphone", 105, 8) {
            public void run() {
                toggleAudioStreaming();
            }
        });

        menu.add(new net.rim.device.api.ui.MenuItem("Centre de Notifications", 110, 9) {
            public void run() {
                app.getUIManager().openNotificationList();
            }
        });
        
        menu.add(new net.rim.device.api.ui.MenuItem("Faire sonner l'Android", 110, 10) {
            public void run() {
                app.getConnectionManager().sendData("FIND_PHONE\n");
                app.getUIManager().showSearchingPhonePopup();
            }
        });

        menu.add(new net.rim.device.api.ui.MenuItem("Guide Audio HFP", 110, 15) {
            public void run() {
                showHfpInfoDialog();
            }
        });
        
        menu.add(new net.rim.device.api.ui.MenuItem("Rafraîchir les cartes SIM", 110, 20) {
            public void run() {
                app.getConnectionManager().sendData("GET_SIMS\n");
            }
        });
        
        menu.add(new net.rim.device.api.ui.MenuItem("Connecter à l'Android", 110, 25) {
            public void run() {
                app.getConnectionManager().connectToPairedDevice();
            }
        });
    }

    // Top status bar matching preview exactly
    private static class TopStatusBarField extends Field {
        private String btText = "[HFP/SPP]";
        private String netText = "[4G] inwi - Signal: [||||]";
        private String batText = "[BB: 88%]";
        private boolean isConnected = true;

        public TopStatusBarField() {
            super(NON_FOCUSABLE);
        }

        public void setBluetoothConnected(boolean connected) {
            this.isConnected = connected;
            this.btText = connected ? "[HFP/SPP]" : "[HFP: OFF]";
            invalidate();
        }

        public void setNetworkInfo(String text) {
            this.netText = text;
            invalidate();
        }

        public void setBatteryLevel(int level) {
            this.batText = "[BB: " + level + "%]";
            invalidate();
        }

        public int getPreferredWidth() { return 320; }
        public int getPreferredHeight() { return 20; }

        protected void layout(int width, int height) {
            setExtent(getPreferredWidth(), getPreferredHeight());
        }

        protected void paint(Graphics graphics) {
            int w = getWidth();
            int h = getHeight();
            
            // Background
            graphics.setColor(0x11161B);
            graphics.fillRect(0, 0, w, h);
            
            // Bottom border
            graphics.setColor(0x1F2937);
            graphics.drawLine(0, h - 1, w, h - 1);

            try {
                graphics.setFont(Font.getDefault().derive(Font.BOLD, 10));
            } catch (Throwable e) {}

            // Left: [HFP/SPP] in Cyan
            graphics.setColor(isConnected ? 0x00E5FF : 0xEF4444);
            graphics.drawText(btText, 4, 3);

            // Center: [4G] inwi - Signal: [||||] in Yellow
            graphics.setColor(0xFACC15);
            Font f = graphics.getFont();
            int netX = (w - f.getAdvance(netText)) / 2;
            graphics.drawText(netText, netX, 3);

            // Right: [BB: 88%] in Green
            graphics.setColor(0x22C55E);
            int batX = w - f.getAdvance(batText) - 4;
            graphics.drawText(batText, batX, 3);
        }
    }
}
