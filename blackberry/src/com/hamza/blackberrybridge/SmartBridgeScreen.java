package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class SmartBridgeScreen extends MainScreen {
    private DarkLabelField clockLabel;
    private DarkLabelField dateLabel;
    private DarkLabelField btStatusLabel;
    private DarkLabelField batteryLabel;
    private DarkLabelField weatherLabel;
    private DarkLabelField networkStatusLabel;
    private DarkButtonField btnNotifs;
    private SmartBridgeApp app;
    private DarkLabelField bbBatteryLabel;
    private Timer uiTimer;
    
    public SmartBridgeScreen(SmartBridgeApp application) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.app = application;
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager header = new VerticalFieldManager(Field.FIELD_HCENTER);
        header.setPadding(4, 4, 4, 4); 
        
        // Compact Status Line: BT status, BB battery, Phone battery
        HorizontalFieldManager statusContainer = new HorizontalFieldManager(Field.FIELD_HCENTER);
        statusContainer.setPadding(2, 0, 2, 0);
        
        btStatusLabel = new DarkLabelField("[BT: WAIT] ", 0xFF3333); 
        bbBatteryLabel = new DarkLabelField("[BB: --%] ", 0xAAAAAA);
        batteryLabel = new DarkLabelField("[Ph: --%]", 0x00FF00); 
        
        try {
            Font smallFont = Font.getDefault().derive(Font.PLAIN, 12);
            btStatusLabel.setFont(smallFont);
            bbBatteryLabel.setFont(smallFont);
            batteryLabel.setFont(smallFont);
        } catch (Throwable e) {}
        
        statusContainer.add(btStatusLabel);
        statusContainer.add(bbBatteryLabel);
        statusContainer.add(batteryLabel);
        header.add(statusContainer);
        
        networkStatusLabel = new DarkLabelField("[Cell] Recherche signal... [....]", Field.FIELD_HCENTER, 0x00D0FF);
        try {
            networkStatusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 12));
        } catch (Throwable e) {}
        header.add(networkStatusLabel);
        
        boolean isNight = app.getSettingsManager().isNightMode();
        int clockColor = isNight ? 0x666666 : Color.WHITE;
        
        // Compact Clock & Date
        clockLabel = new DarkLabelField("--:--", Field.FIELD_HCENTER, clockColor);
        try {
            clockLabel.setFont(Font.getDefault().derive(Font.BOLD, 30));
        } catch (Throwable e) {}
        
        dateLabel = new DarkLabelField("---", Field.FIELD_HCENTER, 0x888888);
        try { dateLabel.setFont(Font.getDefault().derive(Font.PLAIN, 12)); } catch(Exception e){}
        
        weatherLabel = new DarkLabelField("  -- deg C", Field.FIELD_HCENTER, 0x00A2E8); 
        try { weatherLabel.setFont(Font.getDefault().derive(Font.PLAIN, 12)); } catch (Throwable e) {}
        
        header.add(clockLabel);
        header.add(dateLabel);
        header.add(weatherLabel);
        
        add(header);
        
        // --- 3x3 LAUNCHER GRID (Optimized for 320x240 Curve screen) ---
        VerticalFieldManager grid = new VerticalFieldManager(Field.FIELD_HCENTER);
        grid.setPadding(2, 0, 4, 0);
        
        int btnW = 96;  // 3 * 96 = 288px, fits comfortably in 320px screen
        int btnH = 30;  // 3 rows = 90px height
        
        HorizontalFieldManager row1 = new HorizontalFieldManager(Field.FIELD_HCENTER);
        DarkButtonField btnCalls = new DarkButtonField("Calls", btnW, btnH);
        btnCalls.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getUIManager().openDialer(); }
        });
        
        DarkButtonField btnMessages = new DarkButtonField("Messages", btnW, btnH);
        btnMessages.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getConnectionManager().sendData("OPEN_APP|Messages\n"); }
        });
        
        btnNotifs = new DarkButtonField("Notifs (0)", btnW, btnH);
        btnNotifs.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getUIManager().openNotificationList(); }
        });
        
        row1.add(btnCalls);
        row1.add(btnMessages);
        row1.add(btnNotifs);
        grid.add(row1);
        
        HorizontalFieldManager row2 = new HorizontalFieldManager(Field.FIELD_HCENTER);
        row2.setPadding(3, 0, 0, 0);
        DarkButtonField btnWA = new DarkButtonField("WhatsApp", btnW, btnH);
        btnWA.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getConnectionManager().sendData("OPEN_APP|WhatsApp\n"); }
        });

        DarkButtonField btnFB = new DarkButtonField("Messenger", btnW, btnH);
        btnFB.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getConnectionManager().sendData("OPEN_APP|Messenger\n"); }
        });
        
        DarkButtonField btnTG = new DarkButtonField("Telegram", btnW, btnH);
        btnTG.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getConnectionManager().sendData("OPEN_APP|Telegram\n"); }
        });
        
        row2.add(btnWA);
        row2.add(btnFB);
        row2.add(btnTG);
        grid.add(row2);
        
        HorizontalFieldManager row3 = new HorizontalFieldManager(Field.FIELD_HCENTER);
        row3.setPadding(3, 0, 0, 0);
        DarkButtonField btnContacts = new DarkButtonField("Contacts", btnW, btnH);
        btnContacts.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getUIManager().openContacts(); }
        });

        DarkButtonField btnMusic = new DarkButtonField("Music", btnW, btnH);
        btnMusic.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getUIManager().openMedia(); }
        });
        
        DarkButtonField btnSettings = new DarkButtonField("Settings", btnW, btnH);
        btnSettings.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { app.getUIManager().openSettings(); }
        });
        
        row3.add(btnContacts);
        row3.add(btnMusic);
        row3.add(btnSettings);
        grid.add(row3);
        
        add(grid);
        
        updateTimeAndBBBattery();
        
        uiTimer = new Timer();
        uiTimer.schedule(new TimerTask() {
            public void run() {
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() { updateTimeAndBBBattery(); }
                });
            }
        }, 10000, 10000); 
    }
    
    private void updateTimeAndBBBattery() {
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        String time = (h < 10 ? "0" + h : "" + h) + ":" + (m < 10 ? "0" + m : "" + m);
        clockLabel.setText(time);
        
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        dateLabel.setText((day < 10 ? "0"+day : "" + day) + "/" + (month < 10 ? "0"+month : "" + month) + "/" + year);
        
        int bbBat = BatteryManager.getBatteryLevel();
        if (bbBatteryLabel != null) {
            bbBatteryLabel.setText("[BB: " + bbBat + "%]");
        }
        
        // Refresh Night Mode color if changed
        boolean isNight = app.getSettingsManager().isNightMode();
        clockLabel.setColor(isNight ? 0x555555 : Color.WHITE);
    }
    
    public void updateConnectionStatus(String status) {
        if (status.equals("CONNECTED")) {
            btStatusLabel.setText("[BT: ON] ");
            btStatusLabel.setColor(0x00FF00); 
        } else {
            btStatusLabel.setText("[BT: WAIT] ");
            btStatusLabel.setColor(0xFF0000); 
            if (networkStatusLabel != null) {
                networkStatusLabel.setText("[Cell] Deconnecte [....]");
                networkStatusLabel.setColor(0x777777);
            }
        }
    }
    
    public void updateNetworkTelemetry(String operator, String netType, int signalBars, String status) {
        String bars;
        if (signalBars >= 4) {
            bars = "||||";
        } else if (signalBars == 3) {
            bars = "|||.";
        } else if (signalBars == 2) {
            bars = "||..";
        } else if (signalBars == 1) {
            bars = "|...";
        } else {
            bars = "....";
        }

        boolean isOnline = status != null && status.equalsIgnoreCase("ONLINE");
        String statusLabel = isOnline ? "(En ligne)" : "(Hors ligne)";

        String op = (operator != null && operator.length() > 0) ? operator : "Cell";
        if (op.length() > 12) {
            op = op.substring(0, 10) + "..";
        }
        String type = (netType != null && netType.length() > 0) ? netType : "4G";

        String text = "[" + type + "] " + op + " - Signal : [" + bars + "] " + statusLabel;
        if (networkStatusLabel != null) {
            networkStatusLabel.setText(text);
            networkStatusLabel.setColor(isOnline ? 0x00E5FF : 0xFFA500);
        }
    }
    
    public void updateWeather(String temp, String unit, String cond, String city) {
        weatherLabel.setText(temp + "°" + unit + (cond != null && cond.length() > 0 ? " " + cond : ""));
    }
    
    public void updateBattery(String level) {
        batteryLabel.setText("[Ph: " + level + "%]");
    }
    
    public void updateNotificationCount(int count) {
        btnNotifs.setText("Notifs (" + count + ")");
    }
    
    public void addLog(String log) {
    }
    
    public boolean onClose() {
        if (uiTimer != null) {
            uiTimer.cancel();
            uiTimer = null;
        }
        return super.onClose();
    }
    
    protected boolean keyDown(int keycode, int time) {
        return super.keyDown(keycode, time);
    }
    
    protected void makeMenu(net.rim.device.api.ui.component.Menu menu, int instance) {
        super.makeMenu(menu, instance);
        menu.add(new net.rim.device.api.ui.MenuItem("Faire sonner l'Android", 110, 10) {
            public void run() {
                app.getConnectionManager().sendData("FIND_PHONE\n");
                app.getUIManager().showSearchingPhonePopup();
            }
        });
        menu.add(new net.rim.device.api.ui.MenuItem("Arrêter la sonnerie", 110, 11) {
            public void run() {
                app.getConnectionManager().sendData("FIND_PHONE_STOP\n");
                app.getUIManager().hideSearchingPhonePopup();
            }
        });
        menu.add(new net.rim.device.api.ui.MenuItem("Rafraîchir le réseau", 110, 15) {
            public void run() {
                app.getConnectionManager().sendData("GET_NETWORK\n");
            }
        });
        menu.add(new net.rim.device.api.ui.MenuItem("Demander les contacts VIP", 110, 16) {
            public void run() {
                app.getContactManager().requestVipContacts();
                app.getUIManager().openContacts();
            }
        });
        menu.add(new net.rim.device.api.ui.MenuItem("Relancer Bluetooth", 110, 20) {
            public void run() {
                app.getConnectionManager().restartServer();
            }
        });
        menu.add(new net.rim.device.api.ui.MenuItem("Connecter à l'Android", 110, 21) {
            public void run() {
                app.getConnectionManager().connectToPairedDevice();
            }
        });
    }
}
