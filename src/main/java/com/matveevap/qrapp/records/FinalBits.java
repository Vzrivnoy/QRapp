package com.matveevap.qrapp.records;

import java.util.BitSet;

public record FinalBits(BitSet bits, int length) {
    public static FinalBits of(byte[] finalBytes) {
        int length = finalBytes.length * 8;
        BitSet bits = getFinalBits(finalBytes);
        return new FinalBits(bits, length);
    }

    private static BitSet getFinalBits(byte[] bytes) {
        BitSet bits = new BitSet();
        int bitIndex = 0;

        for (byte b : bytes) {
            for (int i = 7; i >= 0; i--) {
                if (((b >> i) & 1) == 1) {
                    bits.set(bitIndex);
                }
                bitIndex++;
            }
        }
        return bits;
    }
}
