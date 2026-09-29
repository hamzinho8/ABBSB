package com.hamza.blackberrybridge.audio;

public class FastBase64 {
    private static final byte[] DECODE_TABLE = new byte[256];
    static {
        for (int i = 0; i < 256; i++) DECODE_TABLE[i] = -1;
        for (int i = 'A'; i <= 'Z'; i++) DECODE_TABLE[i] = (byte)(i - 'A');
        for (int i = 'a'; i <= 'z'; i++) DECODE_TABLE[i] = (byte)(i - 'a' + 26);
        for (int i = '0'; i <= '9'; i++) DECODE_TABLE[i] = (byte)(i - '0' + 52);
        DECODE_TABLE['+'] = 62;
        DECODE_TABLE['/'] = 63;
    }

    public static byte[] decode(String s) {
        if (s == null || s.length() == 0) return new byte[0];
        
        int len = s.length();
        int pad = 0;
        if (len > 0 && s.charAt(len - 1) == '=') pad++;
        if (len > 1 && s.charAt(len - 2) == '=') pad++;
        
        int outLen = (len * 3) / 4 - pad;
        if (outLen <= 0) return new byte[0];
        
        byte[] out = new byte[outLen];
        int outIdx = 0;
        
        for (int i = 0; i < len; i += 4) {
            if (i + 1 >= len) break;
            int c0 = DECODE_TABLE[s.charAt(i) & 0xFF];
            int c1 = DECODE_TABLE[s.charAt(i + 1) & 0xFF];
            int c2 = (i + 2 < len && s.charAt(i + 2) != '=') ? DECODE_TABLE[s.charAt(i + 2) & 0xFF] : 0;
            int c3 = (i + 3 < len && s.charAt(i + 3) != '=') ? DECODE_TABLE[s.charAt(i + 3) & 0xFF] : 0;
            
            if (c0 < 0 || c1 < 0) continue;
            
            int triple = (c0 << 18) | (c1 << 12) | (c2 << 6) | c3;
            
            if (outIdx < outLen) out[outIdx++] = (byte) ((triple >> 16) & 0xFF);
            if (outIdx < outLen) out[outIdx++] = (byte) ((triple >> 8) & 0xFF);
            if (outIdx < outLen) out[outIdx++] = (byte) (triple & 0xFF);
        }
        return out;
    }
}
