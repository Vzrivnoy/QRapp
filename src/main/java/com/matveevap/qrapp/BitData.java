package com.matveevap.qrapp;

import java.util.List;

public record BitData(byte[] bytes, int totalBits) {
    public static BitData of(List<BitField> fields) {
        int totalBits = fields.stream().mapToInt(BitField::bitlength).sum();
        byte[] buffer = new byte[(totalBits + 7) / 8];
        int bitIndex = 0;

        for (BitField field : fields) {
            int value = field.value();
            int len = field.bitlength();

            for (int i = len - 1; i >= 0; i--) {
                int bit = (value >>> i) & 1;
                int byteIndex = bitIndex >>> 3;
                int bitPos = 7 - (bitIndex & 7);
                if (bit == 1)
                    buffer[byteIndex] |= (byte) (1 << bitPos);
                bitIndex++;
            }
        }
        return new BitData(buffer, totalBits);
    }
}
