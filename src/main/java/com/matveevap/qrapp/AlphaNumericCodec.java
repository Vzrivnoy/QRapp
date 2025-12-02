package com.matveevap.qrapp;

import java.util.HashMap;
import java.util.Map;

public class AlphaNumericCodec {

    private static final Map<Character, Integer> CHAR_TO_CODE;
    private static final char[] CODE_TO_CHAR;

    static {
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
        CHAR_TO_CODE = new HashMap<>(alphabet.length());
        CODE_TO_CHAR = new char[alphabet.length()];

        for (int i = 0; i < alphabet.length(); i++) {
            char c = alphabet.charAt(i);
            CHAR_TO_CODE.put(c, i);
            CODE_TO_CHAR[i] = c;
        }
    }

    public static int getCode(char c) {
        return CHAR_TO_CODE.get(c);
    }

    public static String encodeChunk(String s) {
        int a = getCode(s.charAt(0));
        if (s.length() == 2) {
            a = a * 45 + getCode(s.charAt(1));
        }
        return getBinary(a);
    }

    private static String getBinary(int a) {
        StringBuilder s = new StringBuilder();
        s.append(Integer.toBinaryString(a));
        if (a > 44)
            s.insert(0, "0".repeat(11 - s.length()));
        else
            s.insert(0, "0".repeat(6 - s.length()));
        return s.toString();
    }
}
