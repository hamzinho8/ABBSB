package com.hamza.blackberrybridge;

import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.Graphics;

public class DarkLabelField extends LabelField {
    private int color;
    
    public DarkLabelField(String text, long style, int color) {
        super(text, style);
        this.color = color;
    }
    
    public DarkLabelField(String text, int color) {
        super(text, 0);
        this.color = color;
    }
    
    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    protected void paint(Graphics g) {
        g.setColor(color);
        super.paint(g);
    }
}
