package com.hamza.blackberrybridge.audio;

/**
 * Encodeur et Décodeur Base64 ultra-rapide compatible CLDC 1.1 / Java 1.3
 * Sans dépendance externe pour BlackBerry Curve 9300.
 */
public class FastBase64 {
    private static final byte[] DECODE_TABLE = new byte[256];
    private static final char[] ENCODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

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

    public static String decodeString(String s) {
        if (s == null || s.length() == 0) return "";
        byte[] bytes = decode(s);
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            return new String(bytes);
        }
    }

    public static String encode(byte[] data) {
        if (data == null || data.length == 0) return "";
        int len = data.length;
        int rem = len % 3;
        int end = len - rem;
        StringBuffer sb = new StringBuffer((len * 4) / 3 + 4);

        for (int i = 0; i < end; i += 3) {
            int b0 = data[i] & 0xFF;
            int b1 = data[i + 1] & 0xFF;
            int b2 = data[i + 2] & 0xFF;
            sb.append(ENCODE_CHARS[b0 >>> 2]);
            sb.append(ENCODE_CHARS[((b0 & 0x03) << 4) | (b1 >>> 4)]);
            sb.append(ENCODE_CHARS[((b1 & 0x0F) << 2) | (b2 >>> 6)]);
            sb.append(ENCODE_CHARS[b2 & 0x3F]);
        }

        if (rem == 1) {
            int b0 = data[end] & 0xFF;
            sb.append(ENCODE_CHARS[b0 >>> 2]);
            sb.append(ENCODE_CHARS[(b0 & 0x03) << 4]);
            sb.append("==");
        } else if (rem == 2) {
            int b0 = data[end] & 0xFF;
            int b1 = data[end + 1] & 0xFF;
            sb.append(ENCODE_CHARS[b0 >>> 2]);
            sb.append(ENCODE_CHARS[((b0 & 0x03) << 4) | (b1 >>> 4)]);
            sb.append(ENCODE_CHARS[(b1 & 0x0F) << 2]);
            sb.append("=");
        }

        return sb.toString();
    }

    public static String encodeString(String str) {
        if (str == null || str.length() == 0) return "";
        try {
            return encode(str.getBytes("UTF-8"));
        } catch (Exception e) {
            return encode(str.getBytes());
        }
    }
}
