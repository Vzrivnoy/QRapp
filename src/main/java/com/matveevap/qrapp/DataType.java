package com.matveevap.qrapp;

public enum DataType {
    BYTE,
    NUMERIC,
    ALPHANUMERIC;

    static DataType getTypeOfData(String data) {
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
