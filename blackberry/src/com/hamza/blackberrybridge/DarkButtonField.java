package com.hamza.blackberrybridge;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Font;

public class DarkButtonField extends Field {
    public static final int STYLE_SLATE = 0;
    public static final int STYLE_CYAN  = 1;
    public static final int STYLE_GOLD  = 2;
    public static final int STYLE_RED   = 3;
    public static final int STYLE_GREEN = 4;

    private String label;
    private int style = STYLE_SLATE;
    private int width, height;
    
    // Default Slate
    private int bgColor      = 0x1E242B;
    private int borderColor  = 0x374151;
    private int fontColor    = 0xD1D5DB;
    
    // Focus Glowing effect (Trackpad active)
    private int focusBg      = 0x083344;
    private int focusBorder  = 0x00E5FF;
    private int focusFont    = 0xFFFFFF;

    public DarkButtonField(String label, int width, int height) {
        this(label, width, height, STYLE_SLATE);
    }

    public DarkButtonField(String label, int width, int height, int style) {
        super(FOCUSABLE);
        this.label = label;
        this.width = width;
        this.height = height;
        setStyle(style);
    }

    public void setStyle(int style) {
        this.style = style;
        switch (style) {
            case STYLE_CYAN:
                bgColor = 0x16202A;
                borderColor = 0x0284C7;
                fontColor = 0x38BDF8;
                focusBg = 0x0369A1;
                focusBorder = 0x00F0FF;
                break;
            case STYLE_GOLD:
                bgColor = 0x262015;
                borderColor = 0xB45309;
                fontColor = 0xFDE047;
                focusBg = 0x78350F;
                focusBorder = 0xFBBF24;
                break;
            case STYLE_RED:
                bgColor = 0x35161A;
                borderColor = 0xDC2626;
                fontColor = 0xFCA5A5;
                focusBg = 0x991B1B;
                focusBorder = 0xEF4444;
                break;
            case STYLE_GREEN:
                bgColor = 0x14281A;
                borderColor = 0x16A34A;
                fontColor = 0x86EFAC;
                focusBg = 0x166534;
                focusBorder = 0x22C55E;
                break;
            case STYLE_SLATE:
            default:
                bgColor = 0x1E242B;
                borderColor = 0x374151;
                fontColor = 0xD1D5DB;
                focusBg = 0x1E3A5F;
                focusBorder = 0x00E5FF;
                break;
        }
        invalidate();
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
        
        // 1. Background fill with rounded corners (radius 8px)
        graphics.setColor(focused ? focusBg : bgColor);
        graphics.fillRoundRect(0, 0, w, h, 8, 8);
        
        // 2. Border
        if (focused) {
            // Glowing double border on trackpad focus
            graphics.setColor(focusBorder);
            graphics.drawRoundRect(0, 0, w, h, 8, 8);
            graphics.drawRoundRect(1, 1, w - 2, h - 2, 7, 7);
        } else {
            graphics.setColor(borderColor);
            graphics.drawRoundRect(0, 0, w, h, 8, 8);
        }
        
        // 3. Text label centered
        graphics.setColor(focused ? focusFont : fontColor);
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
        if (action == ACTION_INVOKE) {
            fieldChangeNotify(0);
            return true;
        }
        return super.invokeAction(action);
    }
}
