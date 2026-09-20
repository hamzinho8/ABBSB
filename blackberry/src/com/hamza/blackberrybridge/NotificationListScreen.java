package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import java.util.Vector;

public class NotificationListScreen extends MainScreen {
    private NotificationManager notifManager;
    private SmartBridgeApp app;
    private VerticalFieldManager listContainer;
    
    public NotificationListScreen(NotificationManager nm, SmartBridgeApp app) {
        this.notifManager = nm;
        this.app = app;
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        DarkLabelField title = new DarkLabelField("NOTIFICATIONS", Field.FIELD_HCENTER, 0x0078D7);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 24)); } catch(Exception e){}
        add(title);
        add(new SeparatorField());
        
        listContainer = new VerticalFieldManager(Manager.VERTICAL_SCROLL);
        add(listContainer);
        
        refreshList();
    }
    
    public void refreshList() {
        listContainer.deleteAll();
        Vector notifs = notifManager.getNotifications();
        if (notifs.isEmpty()) {
            listContainer.add(new DarkLabelField("No notifications.", Field.FIELD_HCENTER, 0xAAAAAA));
            return;
        }
        
        for (int i = 0; i < notifs.size(); i++) {
            final Notification n = (Notification) notifs.elementAt(notifs.size() - 1 - i); // Latest first
            
            DarkButtonField btn = new DarkButtonField(n.app + " - " + n.sender, 300, 50);
            btn.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    notifManager.openNotification(n);
                }
            });
            listContainer.add(btn);
        }
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { // Red Key or Esc to close
            close();
            return true;
        }
        return super.keyDown(keycode, time);
    }
}
