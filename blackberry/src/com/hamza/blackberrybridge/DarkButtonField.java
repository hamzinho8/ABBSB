package com.hamza.blackberrybridge;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Font;

public class DarkButtonField extends Field {
    private String label;
    private int bgColor = 0x22262E;      // Refined dark slate
    private int borderColor = 0x383E4A;  // Subtle border
    private int focusColor = 0x0078D7;   // Electric BlackBerry / Smartwatch Blue
    private int focusBorder = 0x00A2E8;  // Cyan glow border on focus
    private int fontColor = 0xE0E0E0;
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
        int w = getWidth();
        int h = getHeight();
        
        // Fill button background
        graphics.setColor(focused ? focusColor : bgColor);
        graphics.fillRoundRect(0, 0, w, h, 10, 10);
        
        // Draw crisp 1px border
        graphics.setColor(focused ? focusBorder : borderColor);
        graphics.drawRoundRect(0, 0, w, h, 10, 10);
        
        // Draw label centered
        graphics.setColor(focused ? Color.WHITE : fontColor);
        Font f = graphics.getFont();
        int tx = (w - f.getAdvance(label)) / 2;
        int ty = (h - f.getHeight()) / 2;
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
}
