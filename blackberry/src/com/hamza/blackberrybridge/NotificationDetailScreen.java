package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import java.util.Vector;

public class NotificationDetailScreen extends MainScreen {
    private NotificationManager notifManager;
    private Notification notification;
    private AutoTextEditField replyField;
    private SmartBridgeApp app;
    private VoiceRecorder voiceRecorder;
    
    private DarkButtonField btnRecordVoice;
    private DarkLabelField recordingLabel;
    
    public NotificationDetailScreen(NotificationManager nm, Notification notif, SmartBridgeApp application) {
        super(MainScreen.DEFAULT_MENU | MainScreen.DEFAULT_CLOSE);
        this.notifManager = nm;
        this.notification = notif;
        this.app = application;
        this.voiceRecorder = new VoiceRecorder();
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        DarkLabelField appLabel = new DarkLabelField(notif.app, Field.FIELD_HCENTER, 0x00FF00);
        try { appLabel.setFont(Font.getDefault().derive(Font.BOLD, 22)); } catch(Exception e){}
        add(appLabel);
        
        DarkLabelField senderLabel = new DarkLabelField("From: " + notif.sender, Color.WHITE);
        add(senderLabel);
        add(new SeparatorField());
        
        RichTextField msgField = new RichTextField(notif.message, Field.NON_FOCUSABLE);
        msgField.setFont(Font.getDefault());
        add(msgField);
        
        add(new SeparatorField());
        
        // Use AutoTextEditField for Full T9/QWERTY native BlackBerry experience
        replyField = new AutoTextEditField("Reply: ", "");
        add(replyField);
        
        HorizontalFieldManager actionsRow = new HorizontalFieldManager(Field.FIELD_HCENTER);
        DarkButtonField btnSend = new DarkButtonField("Send Text", 130, 40);
        btnSend.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { sendTextReply(); }
        });
        
        btnRecordVoice = new DarkButtonField("🎤 Hold to Record", 160, 40);
        
        // Advanced Touch/Click handling for Voice Note (Push to talk style or Click to toggle)
        btnRecordVoice.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                toggleVoiceRecording();
            }
        });
        
        actionsRow.add(btnSend);
        actionsRow.add(btnRecordVoice);
        add(actionsRow);
        
        recordingLabel = new DarkLabelField("", Field.FIELD_HCENTER, 0xFF0000);
        add(recordingLabel);
        
        add(new SeparatorField());
        DarkLabelField qrTitle = new DarkLabelField("Quick Replies:", 0x0078D7);
        add(qrTitle);
        
        Vector qr = app.getSettingsManager().getQuickReplies();
        VerticalFieldManager vfmQr = new VerticalFieldManager(Field.FIELD_HCENTER);
        for (int i = 0; i < qr.size(); i++) {
            final String replyText = (String) qr.elementAt(i);
            if (replyText != null && replyText.trim().length() > 0) {
                DarkButtonField btn = new DarkButtonField(replyText, 250, 40);
                btn.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        notifManager.replyToNotification(notification.id, replyText);
                        close();
                    }
                });
                vfmQr.add(btn);
            }
        }
        add(vfmQr);
        
        // Auto Mark Read
        app.getConnectionManager().sendData("NOTIFICATION_ACTION|" + notification.id + "|MARK_READ\n");
    }
    
    private void sendTextReply() {
        String text = replyField.getText();
        if (text != null && text.length() > 0) {
            notifManager.replyToNotification(notification.id, text);
            close();
        }
    }
    
    private void toggleVoiceRecording() {
        if (!voiceRecorder.isRecording()) {
            try {
                voiceRecorder.startRecording();
                btnRecordVoice.setText("🛑 Stop & Send");
                recordingLabel.setText("Recording Audio...");
            } catch (Exception e) {
                LogManager.error("VOICE", "Failed to start: " + e.getMessage());
                recordingLabel.setText("Failed to access Mic");
            }
        } else {
            byte[] audioData = voiceRecorder.stopRecording();
            btnRecordVoice.setText("🎤 Hold to Record");
            recordingLabel.setText("Sending Audio...");
            
            if (audioData != null && audioData.length > 0) {
                notifManager.replyToNotificationVoice(notification.id, audioData);
                close();
            } else {
                recordingLabel.setText("Audio too short or failed.");
            }
        }
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { 
            if (voiceRecorder.isRecording()) {
                voiceRecorder.stopRecording();
            }
            close();
            return true;
        }
        return super.keyDown(keycode, time);
    }
}
