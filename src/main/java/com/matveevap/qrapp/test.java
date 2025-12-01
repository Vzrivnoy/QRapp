package com.matveevap.qrapp;

import java.util.Set;

public class test {
    public static void main(String[] args) {
        System.out.println(newTypeOfData("1"));
        System.out.println(typeOfData("1"));
    }

    static String newTypeOfData(String data) {
        Set<Character> alphaNumericChars = Set.of(
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                'U', 'V', 'W', 'X', 'Y', 'Z',
                ' ', '$', '%', '*', '+', '-', '.', '/', ':'
        );

        boolean hasNonDigit = false;

        for (char c : data.toCharArray()) {
            if (!alphaNumericChars.contains(c)) {
                return "ByteData";
            }
            if (!Character.isDigit(c)) {
                hasNonDigit = true;
            }
        }

        // Если дошли сюда — все символы AlphaNumeric
        return hasNonDigit ? "AlphaNumericData" : "NumericData";
    }

    static String typeOfData(String data) {
        boolean isByteData = false;
        boolean isNumericData = false;
        char[] alphaNumeric = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':'};
        char[] numeric = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        char c;
        int n;
        for (int i = 0; i < data.length(); i++) {
            c = data.charAt(i);
            n = -1;
            for (int j = 0; j < alphaNumeric.length; j++) {
                if (c == alphaNumeric[j]) {
                    n = j;
                    break;
                }
            }
            if (n == -1) {
                isByteData = true;
                break;
            }
        }
        if (isByteData) return "ByteData";
        else {
            for (int i = 0; i < data.length(); i++) {
                c = data.charAt(i);
                n = -1;
                for (int j = 0; j < numeric.length; j++) {
                    if (c == numeric[j]) {
                        n = j;
                        break;
                    }
                }
                if (n == -1) {
                    isNumericData = false;
                    break;
                } else isNumericData = true;
            }
            if (isNumericData) return "NumericData";
            else return "AlphaNumericData";
        }
    }
}
