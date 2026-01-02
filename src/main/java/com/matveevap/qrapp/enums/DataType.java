package com.matveevap.qrapp.enums;

import com.matveevap.qrapp.AlphaNumericCodec;

public enum DataType {
    BYTE,
    NUMERIC,
    ALPHANUMERIC;

    public static DataType getTypeOfData(String data) {
        boolean hasNonDigit = false;

        for (char c : data.toCharArray()) {
            if (!AlphaNumericCodec.isSupported(c)) {
                return DataType.BYTE;
            }
            if (!Character.isDigit(c)) {
                hasNonDigit = true;
            }
        }

        // Если дошли сюда — все символы AlphaNumeric
        return hasNonDigit ? DataType.ALPHANUMERIC : DataType.NUMERIC;
    }
}
