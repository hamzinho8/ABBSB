package com.hamza.blackberrybridge;

import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

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
        pushScreen(screen);
    }
    
    public void pushScreen(final Screen screen) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                UiApplication.getUiApplication().pushScreen(screen);
            }
        });
    }
    
    public void showNewMessagePopup(final String id, final String sender, final String body) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                UiApplication.getUiApplication().pushScreen(new MessagePopupScreen(app, id, sender, body));
            }
        });
    }

    public void updateConnectionStatus(final String status) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                mainScreen.updateConnectionStatus(status);
            }
        });
    }
    
    public void updateBattery(final String level) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                mainScreen.updateBattery(level);
            }
        });
    }
    
    public void updateWeather(final String temp, final String unit, final String cond, final String city) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                mainScreen.updateWeather(temp, unit, cond, city);
            }
        });
    }
    
    public void notifyNewNotification(final Notification n) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
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
    
    public void openCallHistory() {
        pushScreen(new CallHistoryScreen(app));
    }
    
    public void openContacts() {
        pushScreen(new ContactListScreen(app.getContactManager(), app));
    }
    
    public void openMedia() {
        pushScreen(new MediaScreen(app.getMediaManager()));
    }
    
    public void openSettings() {
        pushScreen(new SettingsScreen(app.getSettingsManager(), app));
    }
    
    private FindPhonePopup findPhonePopup;
    private SearchingPhonePopup searchingPhonePopup;
    
    public void showSearchingPhonePopup() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (searchingPhonePopup == null) {
                    searchingPhonePopup = new SearchingPhonePopup(app);
                    UiApplication.getUiApplication().pushScreen(searchingPhonePopup);
                }
            }
        });
    }

    public void hideSearchingPhonePopup() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (searchingPhonePopup != null) {
                    try { searchingPhonePopup.close(); } catch (Exception e) {}
                    searchingPhonePopup = null;
                }
            }
        });
    }

    public void onPhoneFound() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (searchingPhonePopup != null) {
                    try { searchingPhonePopup.close(); } catch (Exception e) {}
                    searchingPhonePopup = null;
                }
                if (findPhonePopup != null) {
                    try { findPhonePopup.close(); } catch (Exception e) {}
                    findPhonePopup = null;
                }
                HardwareManager.stopFindPhoneAlert();
                Dialog.inform("Téléphone retrouvé !");
            }
        });
    }
    
    public void showFindPhonePopup() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (findPhonePopup == null) {
                    findPhonePopup = new FindPhonePopup();
                    UiApplication.getUiApplication().pushScreen(findPhonePopup);
                }
            }
        });
    }
    
    public void hideFindPhonePopup() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (findPhonePopup != null) {
                    try { findPhonePopup.close(); } catch (Exception e) {}
                    findPhonePopup = null;
                }
            }
        });
    }

    public void updateNetworkTelemetry(final String operator, final String netType, final int signalBars, final String status) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (mainScreen != null) {
                    mainScreen.updateNetworkTelemetry(operator, netType, signalBars, status);
                }
            }
        });
    }
}
