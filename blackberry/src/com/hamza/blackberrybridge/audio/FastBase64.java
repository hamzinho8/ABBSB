package com.hamza.blackberrybridge.audio;

import java.io.ByteArrayOutputStream;

/**
 * Décodeur Base64 pur Java 1.3 / CLDC 1.1 ultra-rapide pour BlackBerry OS (Curve 9300).
 * 100% autonome, sans aucune dépendance externe ni RIM Base64InputStream.
 */
public class FastBase64 {
    private static final byte[] DECODE_TABLE = new byte[256];
    
    static {
        for (int i = 0; i < 256; i++) {
            DECODE_TABLE[i] = -1;
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            DECODE_TABLE[i] = (byte)(i - 'A');
        }
        for (int i = 'a'; i <= 'z'; i++) {
            DECODE_TABLE[i] = (byte)(i - 'a' + 26);
        }
        for (int i = '0'; i <= '9'; i++) {
            DECODE_TABLE[i] = (byte)(i - '0' + 52);
        }
        DECODE_TABLE['+'] = 62;
        DECODE_TABLE['/'] = 63;
    }

    public static byte[] decode(String s) {
        if (s == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length() * 3 / 4);
        int buffer = 0;
        int bitsCollected = 0;
        int len = s.length();
        
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '=') break;
            int value = DECODE_TABLE[c & 0xFF];
            if (value >= 0) {
                buffer = (buffer << 6) | value;
                bitsCollected += 6;
                if (bitsCollected >= 8) {
                    bitsCollected -= 8;
                    out.write((buffer >> bitsCollected) & 0xFF);
                }
            }
        }
        return out.toByteArray();
    }
}
