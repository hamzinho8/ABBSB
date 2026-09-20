package com.hamza.blackberrybridge;

import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.rms.RecordStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class SettingsManager {
    private static final String STORE_NAME = "SmartBridgeSettings";
    private Hashtable settings;

    public SettingsManager() {
        settings = new Hashtable();
        loadSettings();
    }

    private void loadSettings() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE_NAME, true);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);
                
                settings.put("auto_reconnect", new Boolean(dis.readBoolean()));
                settings.put("debug_mode", new Boolean(dis.readBoolean()));
                settings.put("night_mode", new Boolean(dis.readBoolean()));
                settings.put("sound_calls", new Boolean(dis.readBoolean()));
                settings.put("vol_calls", new Integer(dis.readInt()));
                settings.put("sound_notifs", new Boolean(dis.readBoolean()));
                settings.put("vol_notifs", new Integer(dis.readInt()));
                settings.put("vibration_enabled", new Boolean(dis.readBoolean()));
                settings.put("profile", dis.readUTF());
                
                int qrCount = dis.readInt();
                Vector qr = new Vector();
                for (int i = 0; i < qrCount; i++) {
                    qr.addElement(dis.readUTF());
                }
                settings.put("quick_replies", qr);
                
                dis.close();
            } else {
                initDefaultSettings();
                saveSettings();
            }
        } catch (Throwable e) {
            initDefaultSettings();
        } finally {
            try { if (rs != null) rs.closeRecordStore(); } catch (Exception ex) {}
        }
    }

    private void initDefaultSettings() {
        settings.put("auto_reconnect", new Boolean(true));
        settings.put("debug_mode", new Boolean(false));
        settings.put("night_mode", new Boolean(false));
        settings.put("sound_calls", new Boolean(true));
        settings.put("vol_calls", new Integer(80));
        settings.put("sound_notifs", new Boolean(true));
        settings.put("vol_notifs", new Integer(70));
        settings.put("vibration_enabled", new Boolean(true));
        settings.put("profile", "Ring+Vibrate");
        
        Vector defaultQr = new Vector();
        defaultQr.addElement("I'm busy right now.");
        defaultQr.addElement("OK.");
        defaultQr.addElement("Call me later.");
        settings.put("quick_replies", defaultQr);
    }

    private void saveSettings() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE_NAME, true);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            dos.writeBoolean(((Boolean)settings.get("auto_reconnect")).booleanValue());
            dos.writeBoolean(((Boolean)settings.get("debug_mode")).booleanValue());
            dos.writeBoolean(((Boolean)settings.get("night_mode")).booleanValue());
            dos.writeBoolean(((Boolean)settings.get("sound_calls")).booleanValue());
            dos.writeInt(((Integer)settings.get("vol_calls")).intValue());
            dos.writeBoolean(((Boolean)settings.get("sound_notifs")).booleanValue());
            dos.writeInt(((Integer)settings.get("vol_notifs")).intValue());
            dos.writeBoolean(((Boolean)settings.get("vibration_enabled")).booleanValue());
            dos.writeUTF((String)settings.get("profile"));
            
            Vector qr = (Vector)settings.get("quick_replies");
            dos.writeInt(qr.size());
            for (int i = 0; i < qr.size(); i++) {
                dos.writeUTF((String)qr.elementAt(i));
            }
            
            byte[] data = baos.toByteArray();
            dos.close();
            
            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (Throwable e) {
            // Ignore
        } finally {
            try { if (rs != null) rs.closeRecordStore(); } catch (Exception ex) {}
        }
    }

    public boolean isAutoReconnect() {
        Boolean b = (Boolean) settings.get("auto_reconnect");
        return b != null ? b.booleanValue() : true;
    }
    public void setAutoReconnect(boolean val) {
        settings.put("auto_reconnect", new Boolean(val));
        saveSettings();
    }
    
    public boolean isDebugMode() {
        Boolean b = (Boolean) settings.get("debug_mode");
        return b != null ? b.booleanValue() : false;
    }
    public void setDebugMode(boolean val) {
        settings.put("debug_mode", new Boolean(val));
        saveSettings();
        LogManager.setDebugEnabled(val); 
    }
    
    public boolean isNightMode() {
        Boolean b = (Boolean) settings.get("night_mode");
        return b != null ? b.booleanValue() : false;
    }
    public void setNightMode(boolean val) {
        settings.put("night_mode", new Boolean(val));
        saveSettings();
    }
    
    public Vector getQuickReplies() {
        Vector qr = (Vector) settings.get("quick_replies");
        if (qr == null) {
            qr = new Vector();
            qr.addElement("OK.");
        }
        return qr;
    }
    public void setQuickReplies(Vector newQr) {
        settings.put("quick_replies", newQr);
        saveSettings();
    }

    public boolean isSoundCalls() {
        Boolean b = (Boolean) settings.get("sound_calls");
        return b != null ? b.booleanValue() : true;
    }
    public void setSoundCalls(boolean val) {
        settings.put("sound_calls", new Boolean(val));
        saveSettings();
    }
    public int getVolCalls() {
        Integer i = (Integer) settings.get("vol_calls");
        return i != null ? i.intValue() : 80;
    }
    public void setVolCalls(int val) {
        settings.put("vol_calls", new Integer(val));
        saveSettings();
    }
    public boolean isSoundNotifs() {
        Boolean b = (Boolean) settings.get("sound_notifs");
        return b != null ? b.booleanValue() : true;
    }
    public void setSoundNotifs(boolean val) {
        settings.put("sound_notifs", new Boolean(val));
        saveSettings();
    }
    public int getVolNotifs() {
        Integer i = (Integer) settings.get("vol_notifs");
        return i != null ? i.intValue() : 70;
    }
    public void setVolNotifs(int val) {
        settings.put("vol_notifs", new Integer(val));
        saveSettings();
    }
    public boolean isVibrationEnabled() {
        Boolean b = (Boolean) settings.get("vibration_enabled");
        return b != null ? b.booleanValue() : true;
    }
    public void setVibrationEnabled(boolean val) {
        settings.put("vibration_enabled", new Boolean(val));
        saveSettings();
    }
    public String getProfile() {
        String p = (String) settings.get("profile");
        return p != null ? p : "Ring+Vibrate";
    }
    public void setProfile(String val) {
        settings.put("profile", val);
        saveSettings();
    }
}
