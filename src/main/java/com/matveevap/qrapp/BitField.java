package com.matveevap.qrapp;

public record BitField(int value, int bitlength) {
    @Override
    public String toString() {
        String s = Integer.toBinaryString(value);
        return "0".repeat(bitlength - s.length()) + s;
    }
}
