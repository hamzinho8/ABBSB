package com.hamza.blackberrybridge;

import net.rim.device.api.system.Application;
import net.rim.device.api.ui.Screen;

public class UIManager {
    private SmartBridgeScreen mainScreen;
    private SmartBridgeApp app;
    private NotificationListScreen notifListScreen;
    
    public UIManager(SmartBridgeApp app) {
        this.app = app;
        mainScreen = new SmartBridgeScreen(app);
    }
    
    public SmartBridgeScreen getMainScreen() {
        return mainScreen;
    }
    
    public void pushGlobalScreen(final Screen screen) {
        net.rim.device.api.system.Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                net.rim.device.api.ui.UiApplication.getUiApplication().pushScreen(screen);
            }
        });
    }
    
    public void pushScreen(final Screen screen) {
        net.rim.device.api.system.Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                net.rim.device.api.ui.UiApplication.getUiApplication().pushScreen(screen);
            }
        });
    }
    
    public void showNewMessagePopup(final String id, final String sender, final String body) {
        Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                net.rim.device.api.ui.UiApplication.getUiApplication().pushScreen(new MessagePopupScreen(app, id, sender, body));
            }
        });
    }

    public void updateConnectionStatus(final String status) {
        Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                mainScreen.updateConnectionStatus(status);
            }
        });
    }
    
    public void updateBattery(final String level) {
        Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                mainScreen.updateBattery(level);
            }
        });
    }
    
    public void updateWeather(final String temp, final String unit, final String cond, final String city) {
        Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                mainScreen.updateWeather(temp, unit, cond, city);
            }
        });
    }
    
    public void notifyNewNotification(final Notification n) {
        Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                mainScreen.addLog("[NOTIF] " + n.app + ": " + n.sender);
                mainScreen.updateNotificationCount(app.getNotificationManager().getNotifications().size());
                if (notifListScreen != null && notifListScreen.isDisplayed()) {
                    notifListScreen.refreshList();
                }
            }
        });
    }
    
    public void openNotificationList() {
        if (notifListScreen == null) {
            notifListScreen = new NotificationListScreen(app.getNotificationManager(), app);
        } else {
            notifListScreen.refreshList();
        }
        pushScreen(notifListScreen);
    }
    
    public void openDialer() {
        pushScreen(new DialerScreen(app, app.getCallManager()));
    }
    
    public void openContacts() {
        pushScreen(new ContactListScreen(app.getContactManager()));
    }
    
    public void openMedia() {
        pushScreen(new MediaScreen(app.getMediaManager()));
    }
    
    public void openSettings() {
        pushScreen(new SettingsScreen(app.getSettingsManager(), app));
    }
    
    private FindPhonePopup findPhonePopup;
    
    public void showFindPhonePopup() {
        Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                if (findPhonePopup == null) {
                    findPhonePopup = new FindPhonePopup();
                    net.rim.device.api.ui.UiApplication.getUiApplication().pushScreen(findPhonePopup);
                }
            }
        });
    }
    
    public void hideFindPhonePopup() {
        Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                if (findPhonePopup != null) {
                    findPhonePopup.close();
                    findPhonePopup = null;
                }
            }
        });
    }
}
