package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

public class SoundSettingsScreen extends MainScreen {
    private SmartBridgeApp app;
    private SettingsManager sm;
    
    private ObjectChoiceField profileField;
    private ObjectChoiceField callVolField;
    private ObjectChoiceField notifVolField;
    private DarkButtonField btnSoundCalls;
    private DarkButtonField btnSoundNotifs;
    private DarkButtonField btnVibration;

    public SoundSettingsScreen(final SmartBridgeApp app) {
        this.app = app;
        this.sm = app.getSettingsManager();
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        DarkLabelField title = new DarkLabelField("SOUND SETTINGS", Field.FIELD_HCENTER, 0x0078D7);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 20)); } catch(Exception e){}
        add(title);
        add(new SeparatorField());
        
        String[] profiles = {"Silent", "Vibrate", "Ring", "Ring+Vibrate"};
        profileField = new ObjectChoiceField("Audio Profile: ", profiles);
        int pIndex = 3;
        String curP = sm.getProfile();
        for (int i=0; i<profiles.length; i++) { if(profiles[i].equals(curP)) pIndex = i; }
        profileField.setSelectedIndex(pIndex);
        add(profileField);
        add(new SeparatorField());
        
        // --- Calls ---
        DarkLabelField lblCalls = new DarkLabelField("--- Calls ---", Field.FIELD_HCENTER, 0xAAAAAA);
        add(lblCalls);
        
        HorizontalFieldManager hfmCallS = new HorizontalFieldManager();
        hfmCallS.add(new DarkLabelField("Call Sounds: ", Color.WHITE));
        btnSoundCalls = new DarkButtonField(sm.isSoundCalls() ? "ON" : "OFF", 100, 30);
        btnSoundCalls.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                btnSoundCalls.setText(btnSoundCalls.getText().equals("ON") ? "OFF" : "ON");
            }
        });
        hfmCallS.add(btnSoundCalls);
        add(hfmCallS);
        
        String[] vols = {"10", "20", "30", "40", "50", "60", "70", "80", "90", "100"};
        callVolField = new ObjectChoiceField("Call Volume: ", vols);
        callVolField.setSelectedIndex(Math.max(0, (sm.getVolCalls() / 10) - 1));
        add(callVolField);
        add(new SeparatorField());
        
        // --- Notifications ---
        DarkLabelField lblNotifs = new DarkLabelField("--- Notifications ---", Field.FIELD_HCENTER, 0xAAAAAA);
        add(lblNotifs);
        
        HorizontalFieldManager hfmNotifS = new HorizontalFieldManager();
        hfmNotifS.add(new DarkLabelField("Notif Sounds: ", Color.WHITE));
        btnSoundNotifs = new DarkButtonField(sm.isSoundNotifs() ? "ON" : "OFF", 100, 30);
        btnSoundNotifs.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                btnSoundNotifs.setText(btnSoundNotifs.getText().equals("ON") ? "OFF" : "ON");
            }
        });
        hfmNotifS.add(btnSoundNotifs);
        add(hfmNotifS);
        
        notifVolField = new ObjectChoiceField("Notif Volume: ", vols);
        notifVolField.setSelectedIndex(Math.max(0, (sm.getVolNotifs() / 10) - 1));
        add(notifVolField);
        
        HorizontalFieldManager hfmVib = new HorizontalFieldManager();
        hfmVib.add(new DarkLabelField("Vibration: ", Color.WHITE));
        btnVibration = new DarkButtonField(sm.isVibrationEnabled() ? "ON" : "OFF", 100, 30);
        btnVibration.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                btnVibration.setText(btnVibration.getText().equals("ON") ? "OFF" : "ON");
            }
        });
        hfmVib.add(btnVibration);
        add(hfmVib);
        
        add(new SeparatorField());
        
        HorizontalFieldManager hfmActions = new HorizontalFieldManager(Field.FIELD_HCENTER);
        DarkButtonField btnTest = new DarkButtonField("Test Sound", 120, 40);
        btnTest.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                saveSettings(); // Save briefly to use correct volume
                app.getAudioManager().testSound();
            }
        });
        DarkButtonField btnSave = new DarkButtonField("Save & Exit", 120, 40);
        btnSave.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                saveSettings();
                close();
            }
        });
        
        hfmActions.add(btnTest);
        hfmActions.add(btnSave);
        add(hfmActions);
    }
    
    private void saveSettings() {
        sm.setProfile((String) profileField.getChoice(profileField.getSelectedIndex()));
        sm.setSoundCalls(btnSoundCalls.getText().equals("ON"));
        sm.setSoundNotifs(btnSoundNotifs.getText().equals("ON"));
        sm.setVibrationEnabled(btnVibration.getText().equals("ON"));
        
        int cv = (callVolField.getSelectedIndex() + 1) * 10;
        int nv = (notifVolField.getSelectedIndex() + 1) * 10;
        sm.setVolCalls(cv);
        sm.setVolNotifs(nv);
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { 
            saveSettings();
            close();
            return true;
        }
        return super.keyDown(keycode, time);
    }
}
