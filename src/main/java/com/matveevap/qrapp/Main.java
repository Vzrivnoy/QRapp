package com.matveevap.qrapp;

import javax.swing.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class Main extends JPanel {
    private static final int SQUARE_SIZE = 25;
    private static final int ROWS = 23;
    private static final int COLUMNS = 23;
    private static final byte[][] QRMATRIX = new byte[21][21];
    private static int numberOfMask = 0;

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        ServiceInfo serviceInfo = QrInputHandler.scanQrParameters(reader);

        String data = serviceInfo.data();
        DataType dataType = serviceInfo.dataType();

        List<BitField> encodedData = getEncodedData(data, dataType);
        BitField serviceDataField = getServiceDataField(serviceInfo);
        byte[] dataWithServiceFields = getDataWithServiceFields(encodedData, serviceDataField, serviceInfo);
        byte[][] chunksOfData = getChunksOfData(dataWithServiceFields, serviceInfo);
        byte[][] chunksOfCorrectionBytes = CorrectionBytes.getCorrectionBytes(chunksOfData, serviceInfo);
        byte[] finalBytes = null;
    }

    static ArrayList<BitField> getEncodedData(String data, DataType dataType) {
        ArrayList<BitField> fields = new ArrayList<>();
        switch (dataType) {
            case BYTE:
                for (byte b : data.getBytes(StandardCharsets.UTF_8))
                    fields.add(new BitField(b, 8));
            case NUMERIC:
                for (int i = 0; i < data.length(); i += 3) {
                    String chunk = data.substring(i, Math.min(i + 3, data.length()));
                    int len = switch (chunk.length()) {
                        case 1 -> 4;
                        case 2 -> 7;
                        case 3 -> 10;
                        default -> throw new AssertionError("len=" + chunk.length());
                    };
                    fields.add(new BitField(Integer.parseInt(chunk), len));
                }
            case ALPHANUMERIC:
                for (int i = 0; i < data.length(); i += 2) {
                    String chunk = data.substring(i, Math.min(i + 2, data.length()));
                    fields.add(new BitField(AlphaNumericCodec.encodeChunk(chunk), chunk.length() == 1 ? 6 : 11));
                }
        }
        return fields;
    }

    static BitField getServiceDataField(ServiceInfo serviceInfo) {
        int encodingMethod = serviceInfo.encodingMethod();
        int bitsOfEncodingMethod = 4;
        int dataLength = serviceInfo.encodedDataLength();
        int bitsOfDataLength = serviceInfo.lengthOfDataLengthField();
        return new BitField((encodingMethod << bitsOfDataLength) | dataLength, bitsOfEncodingMethod + bitsOfDataLength);
    }

    static byte[] getDataWithServiceFields(List<BitField> binaryData, BitField serviceFieldData, ServiceInfo serviceInfo) {
        List<BitField> allFields = new ArrayList<>(binaryData);
        allFields.addFirst(serviceFieldData);
        BitData data = BitData.of(allFields);

        int currentBits = data.totalBits();
        byte[] buffer = data.bytes();

        int remainder = currentBits % 8;
        int padToByte = remainder == 0 ? 0 : (8 - remainder);
        if (padToByte > 0) {
            buffer[buffer.length - 1] <<= padToByte;
            currentBits += padToByte;
        }

        int capacityBits = serviceInfo.maxCapacity();
        if (currentBits < capacityBits) {
            int currentBytes = buffer.length;
            int bytesToAdd = (capacityBits - currentBits) / 8;
            buffer = Arrays.copyOf(buffer, buffer.length + bytesToAdd);

            for (int i = 0; i < bytesToAdd; i++) {
                buffer[currentBytes + i] = (i % 2 == 0) ? (byte) 0b11101100 : 0b00010001;
            }
        }
        return buffer;
    }

    static byte[][] getChunksOfData(byte[] data, ServiceInfo serviceInfo) {
        int[] lengthsOfChunks = getLengthsOfChunks(serviceInfo);
        int numberOfBlocks = serviceInfo.numberOfBlocks();
        byte[][] chunks = new byte[numberOfBlocks][];
        int startIndex = 0;
        for (int i = 0; i < numberOfBlocks; i++) {
            int length = lengthsOfChunks[i];
            chunks[i] = Arrays.copyOfRange(data, startIndex, startIndex + length);
            startIndex += length;
        }
        return chunks;
    }

    static int[] getLengthsOfChunks(ServiceInfo serviceInfo) {
        int dataLengthInBytes = serviceInfo.maxCapacity() / 8;
        int numberOfBlocks = serviceInfo.numberOfBlocks();

        int quotient = dataLengthInBytes / numberOfBlocks;
        int remainder = dataLengthInBytes % numberOfBlocks;

        int[] lengths = new int[numberOfBlocks];
        Arrays.fill(lengths, 0, numberOfBlocks - remainder, quotient);
        Arrays.fill(lengths, numberOfBlocks - remainder, numberOfBlocks, quotient + 1);
        return lengths;
    }

    static byte[] getFinalBytes(byte[][] chunksOfData, byte[][] chunksOfCorrectionBytes, ServiceInfo serviceInfo) {
        int dataLengthInBytes = serviceInfo.maxCapacity();
        int numberOfCorrectionBytes = 0;
        for (byte[] correctionBytes : chunksOfCorrectionBytes)
            numberOfCorrectionBytes += chunksOfCorrectionBytes.length;
        byte[] finalBytes = new byte[dataLengthInBytes + numberOfCorrectionBytes];
        int maxBytes = chunksOfData[chunksOfData.length - 1].length;
        int k = 0;
        for (int byteNumber = 1; byteNumber <= maxBytes; byteNumber++) {
            for (int i = 0; i < chunksOfData.length; i++) {
                if (chunksOfData[i].length < byteNumber)
                    continue;

                finalBytes[k] = chunksOfData[i][byteNumber - 1];
                k++;
            }
        }
        return null;
    }

    /*static void getMatrixWithRightMask() {
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
    }*/

    static void qrOutput() {
        JFrame frame = new JFrame("QR Code");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new Main());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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