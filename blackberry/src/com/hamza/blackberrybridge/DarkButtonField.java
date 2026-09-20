package com.hamza.blackberrybridge;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.TouchEvent;

public class DarkButtonField extends Field {
    private String label;
    private int bgColor = 0x222222; // Dark grey
    private int focusColor = 0x0078D7; // Smartwatch Blue
    private int fontColor = Color.WHITE;
    private int width, height;

    public DarkButtonField(String label, int width, int height) {
        super(FOCUSABLE);
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public String getText() { return label; }

    public void setText(String text) {
        this.label = text;
        invalidate();
    }

    public int getPreferredWidth() { return width; }
    public int getPreferredHeight() { return height; }

    protected void layout(int width, int height) {
        setExtent(getPreferredWidth(), getPreferredHeight());
    }

    protected void paint(Graphics graphics) {
        boolean focused = isFocus();
        graphics.setColor(focused ? focusColor : bgColor);
        graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        
        graphics.setColor(focused ? Color.WHITE : fontColor);
        Font f = graphics.getFont();
        int tx = (getWidth() - f.getAdvance(label)) / 2;
        int ty = (getHeight() - f.getHeight()) / 2;
        graphics.drawText(label, tx, ty);
    }

    protected boolean navigationClick(int status, int time) {
        fieldChangeNotify(0);
        return true;
    }
    
    protected boolean invokeAction(int action) {
        switch(action) {
            case ACTION_INVOKE: { fieldChangeNotify(0); return true; }
        }
        return super.invokeAction(action);
    }
    
    protected boolean touchEvent(TouchEvent message) {
        if(message.getEvent() == TouchEvent.CLICK) {
            fieldChangeNotify(0);
            return true;
        }
        return super.touchEvent(message);
    }
}
