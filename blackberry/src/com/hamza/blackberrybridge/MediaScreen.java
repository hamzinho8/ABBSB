package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.system.Display;

public class MediaScreen extends MainScreen {
    private MediaManager mediaManager;
    private DarkLabelField titleLabel;
    private DarkLabelField artistLabel;
    private DarkLabelField statusLabel;
    
    public MediaScreen(MediaManager manager) {
        super(MainScreen.NO_VERTICAL_SCROLL | MainScreen.NO_HORIZONTAL_SCROLL);
        this.mediaManager = manager;
        manager.setActiveScreen(this);
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager vfm = new VerticalFieldManager(Field.FIELD_HCENTER | Field.USE_ALL_HEIGHT);
        vfm.setPadding(20, 10, 20, 10);
        
        DarkLabelField header = new DarkLabelField("Lecteur Multimédia", Field.FIELD_HCENTER, 0x00A2E8);
        try { header.setFont(Font.getDefault().derive(Font.BOLD, 22)); } catch(Exception e){}
        vfm.add(header);
        vfm.add(new SeparatorField());
        
        VerticalFieldManager spacerTop = new VerticalFieldManager();
        spacerTop.setPadding(30, 0, 0, 0);
        vfm.add(spacerTop);
        
        titleLabel = new DarkLabelField(mediaManager.getTitle(), Field.FIELD_HCENTER, Color.WHITE);
        try { titleLabel.setFont(Font.getDefault().derive(Font.BOLD, 28)); } catch(Exception e){}
        
        artistLabel = new DarkLabelField(mediaManager.getArtist(), Field.FIELD_HCENTER, 0xAAAAAA);
        try { artistLabel.setFont(Font.getDefault().derive(Font.PLAIN, 20)); } catch(Exception e){}
        
        statusLabel = new DarkLabelField(getFormattedState(mediaManager.getState()), Field.FIELD_HCENTER, 0x00FF00);
        try { statusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 18)); } catch(Exception e){}
        
        vfm.add(titleLabel);
        
        VerticalFieldManager midSpacer = new VerticalFieldManager();
        midSpacer.setPadding(5, 0, 15, 0);
        vfm.add(midSpacer);
        
        vfm.add(artistLabel);
        
        VerticalFieldManager stateSpacer = new VerticalFieldManager();
        stateSpacer.setPadding(10, 0, 30, 0);
        vfm.add(stateSpacer);
        
        vfm.add(statusLabel);
        
        HorizontalFieldManager btns = new HorizontalFieldManager(Field.FIELD_HCENTER);
        btns.setPadding(20, 0, 0, 0);
        
        DarkButtonField btnPrev = new DarkButtonField(" |<< ", 80, 50);
        btnPrev.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { mediaManager.previous(); }
        });
        
        HorizontalFieldManager spacer1 = new HorizontalFieldManager();
        spacer1.setPadding(0, 5, 0, 5);
        
        DarkButtonField btnPlayPause = new DarkButtonField(" >/|| ", 120, 50);
        btnPlayPause.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                if ("PLAYING".equals(mediaManager.getState())) {
                    mediaManager.pause();
                } else {
                    mediaManager.play();
                }
            }
        });
        
        HorizontalFieldManager spacer2 = new HorizontalFieldManager();
        spacer2.setPadding(0, 5, 0, 5);
        
        DarkButtonField btnNext = new DarkButtonField(" >>| ", 80, 50);
        btnNext.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { mediaManager.next(); }
        });
        
        btns.add(btnPrev);
        btns.add(spacer1);
        btns.add(btnPlayPause);
        btns.add(spacer2);
        btns.add(btnNext);
        vfm.add(btns);
        
        DarkLabelField hintLabel = new DarkLabelField("(Raccourcis: Espace=Play/Pause, P=Prec, N=Suiv)", Field.FIELD_HCENTER, 0x555555);
        try { hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 14)); } catch(Exception e){}
        VerticalFieldManager hintSpacer = new VerticalFieldManager(Field.FIELD_HCENTER);
        hintSpacer.setPadding(30, 0, 0, 0);
        hintSpacer.add(hintLabel);
        vfm.add(hintSpacer);
        
        add(vfm);
    }
    
    private String getFormattedState(String state) {
        if ("PLAYING".equals(state)) {
            return "\uD83C\uDFB5 Lecture en cours..."; 
        } else if ("PAUSED".equals(state)) {
            return "\u23F8\uFE0F En pause..."; 
        } else if ("STOPPED".equals(state)) {
            return "\u23F9\uFE0F Arrete"; 
        }
        return state;
    }
    
    public void refreshMedia() {
        titleLabel.setText(mediaManager.getTitle());
        artistLabel.setText(mediaManager.getArtist());
        statusLabel.setText(getFormattedState(mediaManager.getState()));
        if ("PLAYING".equals(mediaManager.getState())) {
            statusLabel.setColor(0x00FF00); // Green
        } else {
            statusLabel.setColor(0xFFCC00); // Yellow/Orange
        }
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { 
            mediaManager.setActiveScreen(null);
            close();
            return true;
        } else if (key == Keypad.KEY_SPACE) {
            if ("PLAYING".equals(mediaManager.getState())) {
                mediaManager.pause();
            } else {
                mediaManager.play();
            }
            return true;
        } else if (key == 'n' || key == 'N') {
            mediaManager.next();
            return true;
        } else if (key == 'p' || key == 'P') {
            mediaManager.previous();
            return true;
        }
        return super.keyDown(keycode, time);
    }
}
