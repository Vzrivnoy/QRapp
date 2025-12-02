package com.matveevap.qrapp;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;

import javax.swing.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Set;

public class Main extends JPanel {
    private static final int SQUARE_SIZE = 25;
    private static final int ROWS = 23;
    private static final int COLUMNS = 23;
    private static final String[][] QRMATRIX = new String[21][21];
    private static int numberOfMask = 0;

    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Type data you want to transform into QR code below");
        System.out.print(">>>");

        String data;
        DataType typeOfData;
        String binaryData;
        String serviceFieldData;
        String finalData;
        while (true) {
            data = reader.readLine();
            typeOfData = typeOfData(data);
            binaryData = dataToBinaryCode(data, typeOfData);
            serviceFieldData = getServiceFieldData(data, binaryData, typeOfData);
            finalData = getFinalData(binaryData, serviceFieldData);
            if (finalData.length() > 152) {
                System.out.println("Error. Maximum number of digits: 41, of Alphabetic-Numeric (digits, capital english letters and some special symbols) \nsymbols: 25, of other symbols: 17. " +
                        "Try to reduce the length of your message");
                System.out.print(">>>");
            } else {
                break;
            }
        }

        String binaryCorrectionBytes = getBinaryCorrectionBytes(finalData);
        String realFinalData = finalData + binaryCorrectionBytes;
        String[][] matrix = new String[21][21];
        for (int i = 0; i < 21; i++) {
            for (int j = 0; j < 21; j++) {
                matrix[i][j] = "0";
            }
        }
        for (int i = 0; i < 21; i++) {
            System.arraycopy(matrix[i], 0, QRMATRIX[i], 0, 21);
        }
        qrCodeMakerFirst(realFinalData);
        getMatrixWithRightMask();
        qrCodeMakerSecond();
        addPositionMarkersAndLinesOfSync();
        qrOutput();
        System.out.println("\nYour QR code was successfully made!");
    }

    static DataType typeOfData(String data) {
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
                return DataType.BYTE;
            }
            if (!Character.isDigit(c)) {
                hasNonDigit = true;
            }
        }

        // Если дошли сюда — все символы AlphaNumeric
        return hasNonDigit ? DataType.ALPHANUMERIC : DataType.NUMERIC;
    }

    static String dataToBinaryCode(String data, DataType typeOfData) {
        StringBuilder binaryCode = new StringBuilder();
        switch (typeOfData) {
            case DataType.BYTE:
                for (char chr : data.toCharArray())
                    binaryCode.append(getUTF8Code(chr));
                break;
            case DataType.NUMERIC:
                for (int i = 0; i < data.length(); i += 3) {
                    String chunk = data.substring(i, Math.min(i + 3, data.length()));
                    binaryCode.append(intToBinary(chunk));
                }
                break;
            case DataType.ALPHANUMERIC:
                for (int i = 0; i < data.length(); i += 2) {
                    String chunk = data.substring(i, Math.min(i + 2, data.length()));
                    binaryCode.append(AlphaNumericCodec.encodeChunk(chunk));
                }
        }
        return binaryCode.toString();
    }

    static String getServiceFieldData(String data, String binaryData, DataType typeOfData) {
        int n = 0;

        String fieldOfData = switch (typeOfData) {
            case DataType.BYTE -> "0100";
            case DataType.NUMERIC -> "0001";
            case DataType.ALPHANUMERIC -> "0010";
        };
        StringBuilder dataLength = new StringBuilder(switch (typeOfData) {
            case DataType.BYTE -> simpleIntToBinary(binaryData.length() / 8);
            case DataType.NUMERIC, DataType.ALPHANUMERIC -> simpleIntToBinary(data.length());
        });

        if (typeOfData == DataType.BYTE) n = 8;
        if (typeOfData == DataType.ALPHANUMERIC) n = 9;
        if (typeOfData == DataType.NUMERIC) n = 10;
        if (dataLength.length() < n) {
            n = n - dataLength.length();
            for (int i = 0; i < n; i++) {
                dataLength.insert(0, "0");
            }
        }
        return fieldOfData + dataLength;
    }

    static String getFinalData(String binaryData, String serviceFieldData) {
        StringBuilder finalData = new StringBuilder(serviceFieldData + binaryData);
        int n;

        if (finalData.length() <= 150) finalData.append("0000");
        if (finalData.length() % 8 != 0) {
            n = 8 - (finalData.length() % 8);
            finalData.append("0".repeat(n));
        }
        if (finalData.length() < 152) {
            n = (152 / 8) - finalData.length() / 8;
            for (int i = 0; i < n; i++) {
                if (i % 2 == 0) finalData.append("11101100");
                else finalData.append("00010001");
            }
        }
        return finalData.toString();
    }

    static String getBinaryCorrectionBytes(String finalData) {
        StringBuilder binaryCorrectionBytes = new StringBuilder();
        StringBuilder s;
        int n;
        int[] correctionBytes = getCorrectionBytes(finalData);
        for (int correctionByte : correctionBytes) {
            s = new StringBuilder(simpleIntToBinary(correctionByte));
            if (s.length() % 8 != 0) {
                n = 8 - (s.length() % 8);
                for (int j = 0; j < n; j++) {
                    s.insert(0, "0");
                }
            }
            binaryCorrectionBytes.append(s);
        }
        return binaryCorrectionBytes.toString();
    }

    static void getMatrixWithRightMask() {
        String[][] maskedMatrix = new String[21][21];
        String[][] matrixWithRightMask = new String[21][21];
        StringBuilder s = new StringBuilder();
        String s1, s2;
        int sameElementsInRow = 0;
        int sameElementsInColumn = 0;
        int minFine = MAX_VALUE;
        int fine = 0;
        int blackElements = 0;
        for (int m = 1; m < 9; m++) {
            for (int i = 0; i < 21; i++) {
                System.arraycopy(getMaskedMatrix(QRMATRIX, m)[i], 0, maskedMatrix[i], 0, 21);
            }
            for (int i = 0; i < 21; i++) { //1st and 3rd conditions
                for (int j = 1; j < 21; j++) {
                    //1st condition
                    if (maskedMatrix[i][j].equals(maskedMatrix[i][j - 1])) {
                        if (sameElementsInRow == 0) sameElementsInRow += 2;
                        else sameElementsInRow++;
                    } else {
                        if (sameElementsInRow >= 5) fine += sameElementsInRow - 2;
                        sameElementsInRow = 0;
                    }
                    if (j == 20) {
                        if (sameElementsInRow >= 5) fine += sameElementsInRow - 2;
                        sameElementsInRow = 0;
                    }

                    if (maskedMatrix[j][i].equals(maskedMatrix[j - 1][i])) {
                        if (sameElementsInColumn == 0) sameElementsInColumn += 2;
                        else sameElementsInColumn++;
                    } else {
                        if (sameElementsInColumn >= 5) fine += sameElementsInColumn - 2;
                        sameElementsInColumn = 0;
                    }
                    if (j == 20) {
                        if (sameElementsInColumn >= 5) fine += sameElementsInColumn - 2;
                        sameElementsInColumn = 0;
                    }
                    //3rd condition
                    if (j >= 14) {
                        for (int k = j - 14; k <= j; k++) {
                            s.append(maskedMatrix[i][k]);
                        }
                        s1 = s.substring(0, 11);
                        s2 = s.substring(4, 15);
                        if (s1.equals("00001011101") || s1.equals("10111010000")) fine += 40;
                        if (s2.equals("00001011101") || s2.equals("10111010000")) fine += 40;
                        s = new StringBuilder();
                    }
                    if (i >= 14) {
                        for (int k = i - 14; k <= i; k++) {
                            s.append(maskedMatrix[k][j]);
                        }
                        s1 = s.substring(0, 11);
                        s2 = s.substring(4, 15);
                        if (s1.equals("00001011101") || s1.equals("10111010000")) fine += 40;
                        if (s2.equals("00001011101") || s2.equals("10111010000")) fine += 40;
                        s = new StringBuilder();
                    }
                }
            }
            for (int i = 1; i < 21; i++) {
                for (int j = 1; j < 21; j++) {
                    //2nd condition
                    if (Objects.equals(maskedMatrix[i - 1][j - 1], maskedMatrix[i][j]) && maskedMatrix[i - 1][j].equals(maskedMatrix[i][j]) && maskedMatrix[i][j - 1].equals(maskedMatrix[i][j])) {
                        fine += 3;
                    }
                }
            }
            for (int i = 0; i < 21; i++) {
                for (int j = 0; j < 21; j++) {
                    if (maskedMatrix[i][j].equals("1")) blackElements++;
                }
            }

            //4th condition
            fine += 2 * (((int) ((blackElements / 441.0) * 100 - 50)) >= 0 ? (int) ((blackElements / 441.0) * 100 - 50) : (int) ((blackElements / 441.0) * 100 - 50) * (-1));

            if (fine < minFine) {
                minFine = fine;
                for (int i = 0; i < 21; i++) {
                    System.arraycopy(maskedMatrix[i], 0, matrixWithRightMask[i], 0, 21);
                }
                numberOfMask = m - 1;
            }
            fine = 0;
        }
        for (int i = 0; i < 21; i++) {
            System.arraycopy(matrixWithRightMask[i], 0, QRMATRIX[i], 0, 21);
        }
    }

    static void qrCodeMakerFirst(String realFinalData) {
        int n = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 12; j++) {
                QRMATRIX[20 - j][20 - 4 * i] = realFinalData.charAt(n) + "";
                n++;
                QRMATRIX[20 - j][20 - 4 * i - 1] = realFinalData.charAt(n) + "";
                n++;
            }
            for (int j = 9; j < 21; j++) {
                QRMATRIX[j][18 - 4 * i] = realFinalData.charAt(n) + "";
                n++;
                QRMATRIX[j][18 - 4 * i - 1] = realFinalData.charAt(n) + "";
                n++;
            }
        }
        for (int j = 20; j >= 0; j--) {
            if (j == 6) continue;
            QRMATRIX[j][12] = realFinalData.charAt(n) + "";
            n++;
            QRMATRIX[j][11] = realFinalData.charAt(n) + "";
            n++;
        }
        for (int j = 0; j < 21; j++) {
            if (j == 6) continue;
            QRMATRIX[j][10] = realFinalData.charAt(n) + "";
            n++;
            QRMATRIX[j][9] = realFinalData.charAt(n) + "";
            n++;
        }
        for (int i = 0; i < 4; i++) {
            QRMATRIX[12 - i][8] = realFinalData.charAt(n) + "";
            n++;
            QRMATRIX[12 - i][7] = realFinalData.charAt(n) + "";
            n++;
        }
        for (int i = 0; i < 3; i++) {
            if (i == 0 || i == 2) {
                for (int j = 0; j < 4; j++) {
                    QRMATRIX[12 - j][5 - 2 * i] = realFinalData.charAt(n) + "";
                    n++;
                    QRMATRIX[12 - j][5 - 2 * i - 1] = realFinalData.charAt(n) + "";
                    n++;
                }
            } else {
                for (int j = 9; j < 13; j++) {
                    QRMATRIX[j][3] = realFinalData.charAt(n) + "";
                    n++;
                    QRMATRIX[j][2] = realFinalData.charAt(n) + "";
                    n++;
                }
            }
        }
    }

    static void qrCodeMakerSecond() {
        String[] codeOfMaskAndLevelOfCorrection = new String[]{"111011111000100", "111001011110011", "111110110101010", "111100010011101", "110011000101111", "110001100011000", "110110001000001", "110100101110110"};
        String data = codeOfMaskAndLevelOfCorrection[numberOfMask];
        QRMATRIX[13][8] = "1";
        for (int i = 0; i < 8; i++) {
            QRMATRIX[8][i] = data.charAt(i) + "";
            if (i >= 6) QRMATRIX[8][i + 1] = data.charAt(i) + "";
        }
        for (int i = 0; i < 7; i++) {
            QRMATRIX[i][8] = data.charAt(data.length() - i - 1) + "";
            if (i == 6) QRMATRIX[i + 1][8] = data.charAt(data.length() - i - 2) + "";
        }
        for (int i = 0; i < 7; i++) {
            QRMATRIX[20 - i][8] = data.charAt(i) + "";
        }
        for (int i = 7; i < 15; i++) {
            QRMATRIX[8][6 + i] = data.charAt(i) + "";
        }
    }

    static void addPositionMarkersAndLinesOfSync() {
        String[][] matrix = QRMATRIX;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                if (!(i != 0 && i != 6 && (j == 1 || j == 5))) matrix[i][j] = "1";
                if ((i == 1 || i == 5) && (j < 6 && j > 0)) matrix[i][j] = "0";
                matrix[20 - i][j] = matrix[i][j];
                matrix[i][20 - j] = matrix[i][j];
            }
        }
        for (int i = 8; i < 13; i++) {
            if (i % 2 == 0) {
                matrix[i][6] = "1";
                matrix[6][i] = "1";
            }
        }
    }

    static void qrOutput() {
        JFrame frame = new JFrame("QR Code");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new Main());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static String simpleIntToBinary(int number) {
        if (number == 0) return "00000000";
        StringBuilder s = new StringBuilder();
        StringBuilder s1 = new StringBuilder();
        while (number >= 1) {
            s.append(number % 2);
            number = number / 2;
        }
        for (int i = s.length() - 1; i >= 0; i--) {
            s1.append(s.charAt(i));
        }
        return s1.toString();
    }

    static String intToBinary(String numberInt) {
        int number = Integer.parseInt(numberInt);
        int a = number;
        int n = 0;
        StringBuilder s0 = new StringBuilder();
        StringBuilder s = new StringBuilder();
        StringBuilder s1 = new StringBuilder();
        while (number >= 1) {
            s.append(number % 2);
            number = number / 2;
        }
        for (int i = s.length() - 1; i >= 0; i--) {
            s1.append(s.charAt(i));
        }
        if (s1.length() < 10 && a > 99 && a < 1000) n = 10 - s1.length();
        if (s1.length() < 7 && a > 9 && a < 100) n = 7 - s1.length();
        if (s1.length() < 4 && a < 10) n = 4 - s1.length();
        s0.append("0".repeat(n));
        return s0.toString() + s1;
    }

    static String intToBinaryForUTF8(String numberInt) {
        int number = Integer.parseInt(numberInt);
        int a = number;
        int n = 0;
        StringBuilder s = new StringBuilder();
        StringBuilder s1 = new StringBuilder();
        while (number >= 1) {
            s.append(number % 2);
            number = number / 2;
        }
        for (int i = s.length() - 1; i >= 0; i--) {
            s1.append(s.charAt(i));
        }
        if (a < 128 && s1.length() < 7) n = 7 - s1.length();
        if (a >= 128 && a < 2048 && s1.length() < 11) n = 11 - s1.length();
        if (a >= 2048 && a < 65536 && s1.length() < 16) n = 16 - s1.length();
        for (int i = 0; i < n; i++) {
            s1.insert(0, "0");
        }
        return s1.toString();
    }

    static String getUTF8Code(char symbol) {
        String UTF8Code;
        String binaryCode = intToBinaryForUTF8((int) symbol + "");
        if ((int) symbol < 128) UTF8Code = "0" + binaryCode;
        else if ((int) symbol < 2048) {
            UTF8Code = "110" + binaryCode.substring(0, 5) + "10" + binaryCode.substring(5, 11);
        } else
            UTF8Code = "1110" + binaryCode.substring(0, 4) + "10" + binaryCode.substring(4, 10) + "10" + binaryCode.substring(10, 16);
        return UTF8Code;
    }

    static String[][] getMaskedMatrix(String[][] matrix, int numberOfMask) {
        String[][] maskedMatrix = new String[21][21];
        for (int i = 0; i < 21; i++) {
            System.arraycopy(matrix[i], 0, maskedMatrix[i], 0, 21);
        }

        boolean condition = false;
        for (int i = 0; i < 21; i++) {
            for (int j = 0; j < 21; j++) {
                condition = switch (numberOfMask) {
                    case 1 -> (i + j) % 2 == 0;
                    case 2 -> i % 2 == 0;
                    case 3 -> j % 3 == 0;
                    case 4 -> (i + j) % 3 == 0;
                    case 5 -> (i / 2 + j / 3) % 2 == 0;
                    case 6 -> (i * j) % 2 + (i * j) % 3 == 0;
                    case 7 -> ((i * j) % 3 + (i * j) % 2) % 2 == 0;
                    case 8 -> ((i * j) % 3 + (i + j) % 2) % 2 == 0;
                    default -> condition;
                };
                if (i < 9 && j < 9) continue;
                if (i == 6 || j == 6) continue;
                if ((i >= 13 && j < 9) || (i < 9 && j >= 13)) continue;
                if (condition) {
                    if (maskedMatrix[i][j].equals("1")) maskedMatrix[i][j] = "0";
                    else maskedMatrix[i][j] = "1";
                }
            }
        }
        return maskedMatrix;
    }

    static int[] getListOfFinalBytes(String finalData) {
        StringBuilder bytes = new StringBuilder();
        int[] matrixOfFinalBytes = new int[finalData.length() / 8];
        for (int i = 0; i < finalData.length(); i++) {
            bytes.append(finalData.charAt(i));
            if (bytes.length() == 8) {
                for (int j = 0; j < 8; j++) {
                    matrixOfFinalBytes[i / 8] += (int) (Character.getNumericValue(bytes.charAt(j)) * Math.pow(2, 7 - j));
                }
                bytes = new StringBuilder();
            }
        }
        return matrixOfFinalBytes;
    }

    static int[] getCorrectionBytes(String finalData) {
        int a, b, c;
        int numberOfCorrectionBytes = 7;
        int numberOfBytesInBlock = 19;
        int[] correctionBytes;
        int[] matrix = new int[max(numberOfCorrectionBytes, numberOfBytesInBlock)];
        int[] matrixOfFinalBytes = getListOfFinalBytes(finalData);
        int[] generatingPolynomial = new int[]{87, 229, 146, 149, 238, 102, 21};
        int[] galuaField = new int[]{
                1, 2, 4, 8, 16, 32, 64, 128, 29, 58, 116, 232, 205, 135, 19, 38,
                76, 152, 45, 90, 180, 117, 234, 201, 143, 3, 6, 12, 24, 48, 96, 192,
                157, 39, 78, 156, 37, 74, 148, 53, 106, 212, 181, 119, 238, 193, 159, 35,
                70, 140, 5, 10, 20, 40, 80, 160, 93, 186, 105, 210, 185, 111, 222, 161,
                95, 190, 97, 194, 153, 47, 94, 188, 101, 202, 137, 15, 30, 60, 120, 240,
                253, 231, 211, 187, 107, 214, 177, 127, 254, 225, 223, 163, 91, 182, 113, 226,
                217, 175, 67, 134, 17, 34, 68, 136, 13, 26, 52, 104, 208, 189, 103, 206,
                129, 31, 62, 124, 248, 237, 199, 147, 59, 118, 236, 197, 151, 51, 102, 204,
                133, 23, 46, 92, 184, 109, 218, 169, 79, 158, 33, 66, 132, 21, 42, 84,
                168, 77, 154, 41, 82, 164, 85, 170, 73, 146, 57, 114, 228, 213, 183, 115,
                230, 209, 191, 99, 198, 145, 63, 126, 252, 229, 215, 179, 123, 246, 241, 255,
                227, 219, 171, 75, 150, 49, 98, 196, 149, 55, 110, 220, 165, 87, 174, 65,
                130, 25, 50, 100, 200, 141, 7, 14, 28, 56, 112, 224, 221, 167, 83, 166,
                81, 162, 89, 178, 121, 242, 249, 239, 195, 155, 43, 86, 172, 69, 138, 9,
                18, 36, 72, 144, 61, 122, 244, 245, 247, 243, 251, 235, 203, 139, 11, 22,
                44, 88, 176, 125, 250, 233, 207, 131, 27, 54, 108, 216, 173, 71, 142, 1
        };
        int[] inversedGaluaField = new int[]{
                -1, 0, 1, 25, 2, 50, 26, 198, 3, 223, 51, 238, 27, 104, 199, 75,
                4, 100, 224, 14, 52, 141, 239, 129, 28, 193, 105, 248, 200, 8, 76, 113,
                5, 138, 101, 47, 225, 36, 15, 33, 53, 147, 142, 218, 240, 18, 130, 69,
                29, 181, 194, 125, 106, 39, 249, 185, 201, 154, 9, 120, 77, 228, 114, 166,
                6, 191, 139, 98, 102, 221, 48, 253, 226, 152, 37, 179, 16, 145, 34, 136,
                54, 208, 148, 206, 143, 150, 219, 189, 241, 210, 19, 92, 131, 56, 70, 64,
                30, 66, 182, 163, 195, 72, 126, 110, 107, 58, 40, 84, 250, 133, 186, 61,
                202, 94, 155, 159, 10, 21, 121, 43, 78, 212, 229, 172, 115, 243, 167, 87,
                7, 112, 192, 247, 140, 128, 99, 13, 103, 74, 222, 237, 49, 197, 254, 24,
                227, 165, 153, 119, 38, 184, 180, 124, 17, 68, 146, 217, 35, 32, 137, 46,
                55, 63, 209, 91, 149, 188, 207, 205, 144, 135, 151, 178, 220, 252, 190, 97,
                242, 86, 211, 171, 20, 42, 93, 158, 132, 60, 57, 83, 71, 109, 65, 162,
                31, 45, 67, 216, 183, 123, 164, 118, 196, 23, 73, 236, 127, 12, 111, 246,
                108, 161, 59, 82, 41, 157, 85, 170, 251, 96, 134, 177, 187, 204, 62, 90,
                203, 89, 95, 176, 156, 169, 160, 81, 11, 245, 22, 235, 122, 117, 44, 215,
                79, 174, 213, 233, 230, 231, 173, 232, 116, 214, 244, 234, 168, 80, 88, 175
        };
        for (int i = 0; i < matrix.length; i++) {
            if (i + 1 <= matrixOfFinalBytes.length) matrix[i] = matrixOfFinalBytes[i];
            else matrix[i] = 0;
        }
        for (int i = 0; i < matrixOfFinalBytes.length; i++) {
            a = matrix[0];
            for (int j = 1; j < matrix.length; j++) {
                matrix[j - 1] = matrix[j];
            }
            matrix[matrix.length - 1] = 0;

            if (a == 0) continue;

            b = inversedGaluaField[a];

            for (int j = 0; j < numberOfCorrectionBytes; j++) {
                c = b + generatingPolynomial[j];
                if (c > 254) c = c % 255;
                matrix[j] = matrix[j] ^ galuaField[c];
            }
        }
        correctionBytes = new int[numberOfCorrectionBytes];
        System.arraycopy(matrix, 0, correctionBytes, 0, numberOfCorrectionBytes);
        return correctionBytes;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                if (row == 0 || row == ROWS - 1 || col == 0 || col == COLUMNS - 1) g.setColor(Color.WHITE);
                else if (Objects.equals(QRMATRIX[row - 1][col - 1], "0")) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Color.BLACK);
                }
                g.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(COLUMNS * SQUARE_SIZE, ROWS * SQUARE_SIZE);
    }

}