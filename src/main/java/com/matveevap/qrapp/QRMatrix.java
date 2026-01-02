package com.matveevap.qrapp;

import com.matveevap.qrapp.enums.CorrectionLevel;
import com.matveevap.qrapp.records.FinalBits;
import com.matveevap.qrapp.records.ServiceInfo;

import java.util.BitSet;

public class QRMatrix {
    public static boolean[][] getQRMatrix(FinalBits finalBits, ServiceInfo serviceInfo) {
        int size = 21 + 4 * (serviceInfo.version() - 1);
        int version = serviceInfo.version();
        boolean[][] qrMatrix = new boolean[size][size];
        boolean[][] isServiceBit = new boolean[size][size];

        placeSearchPatterns(qrMatrix, isServiceBit);
        placeAllAlignmentPatterns(qrMatrix, isServiceBit, serviceInfo);
        placeSyncBands(qrMatrix, isServiceBit);
        placeCodeOfVersion(qrMatrix, isServiceBit, serviceInfo);
        placeBlackSquare(qrMatrix, isServiceBit);
        reservePlaceForCodeOfMaskAndCorrectionLevel(isServiceBit);
        fillData(version, qrMatrix, isServiceBit, finalBits);

        return getMatrixWithBestMaskAndCorrectionLevelCode(qrMatrix, isServiceBit, serviceInfo);
    }

    private static void placeSearchPatterns(boolean[][] matrix, boolean[][] mask) {
        int size = matrix.length;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                boolean isOuter = ((i == 0 || i == 6) && j != 7) || ((j == 0 || j == 6) && i != 7);
                boolean isCenter = i >= 2 && i <= 4 && j >= 2 && j <= 4;
                if (isOuter || isCenter) {
                    matrix[i][j] = true;
                    matrix[i][size - j - 1] = true;
                    matrix[size - i - 1][j] = true;
                }
                mask[i][j] = true;
                mask[i][size - j - 1] = true;
                mask[size - i - 1][j] = true;
            }
        }
    }

    private static void placeAllAlignmentPatterns(boolean[][] matrix, boolean[][] mask, ServiceInfo serviceInfo) {
        int version = serviceInfo.version();
        int[] locations = getAlignmentPatterLocations(version);
        int last = locations.length - 1;
        for (int x : locations) {
            for (int y : locations) {
                if (version >= 7 && (x == locations[0] && y == locations[0] ||
                    x == locations[0] && y == locations[last] || 
                    x == locations[last] && y == locations[0]))
                    continue;
                
                placeAlignmentPattern(matrix, mask, x, y);
            }
        }
    }

    private static int[] getAlignmentPatterLocations(int version) {
        int[][] locations = new int[][] {
                {}, {18}, {22}, {26}, {30}, {34}, {6, 22, 38}, {6, 24, 42}, {6, 26, 46}, {6, 28, 50}, {6, 30, 54},
                {6, 32, 58}, {6, 34, 62}, {6, 26, 46, 66}, {6, 26, 48, 70}, {6, 26, 50, 74}, {6, 30, 54, 78},
                {6, 30, 56, 82}, {6, 30, 58, 86}, {6, 34, 62, 90}, {6, 28, 50, 72, 94}, {6, 26, 50, 74, 98},
                {6, 30, 54, 78, 102}, {6, 28, 54, 80, 106}, {6, 32, 58, 84, 110}, {6, 30, 58, 86, 114},
                {6, 34, 62, 90, 118}, {6, 26, 50, 74, 98, 122}, {6, 30, 54, 78, 102, 126}, {6, 26, 52, 78, 104, 130},
                {6, 30, 56, 82, 108, 134}, {6, 34, 60, 86, 112, 138}, {6, 30, 58, 86, 114, 142}, {6, 34, 62, 90, 118, 146},
                {6, 30, 54, 78, 102, 126, 150}, {6, 24, 50, 76, 102, 128, 154}, {6, 28, 54, 80, 106, 132, 158},
                {6, 32, 58, 84, 110, 136, 162}, {6, 26, 54, 82, 110, 138, 166}, {6, 30, 58, 86, 114, 142, 170}
        };
        return locations[version - 1];
    }

    private static void placeAlignmentPattern(boolean[][] matrix, boolean[][] mask, int x, int y) {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                boolean isOuter = (i == 0 || i == 4 || j == 0 || j == 4);
                boolean isCenter = (i == 2 && j == 2);
                matrix[i + x - 2][j + y - 2] = (isOuter || isCenter);
                mask[i + x - 2][j + y - 2] = true;
            }
        }
    }

    private static void placeSyncBands(boolean[][] matrix, boolean[][] mask) {
        int size = matrix.length;
        int x = 6;
        for (int i = 8; i < size - 8; i++) {
            if (!mask[i][6]) {
                matrix[i][x] = (i % 2 == 0);
                matrix[x][i] = (i % 2 == 0);
                mask[i][x] = true;
                mask[x][i] = true;
            }
        }
    }

    private static void placeCodeOfVersion(boolean[][] matrix, boolean[][] mask, ServiceInfo serviceInfo) {
        int version = serviceInfo.version();

        if (version < 7)
            return;

        int codeOfVersion = getCodeOfVersion(version);
        int size = matrix.length;
        int codeBits = 17;
        int bit = 0;
        for (int i = size - 11; i < size - 8; i++) {
            for (int j = 0; j < 6; j++) {
                int currentBit = (codeOfVersion >> (codeBits - bit) & 1);
                if (currentBit == 1) {
                    matrix[i][j] = true;
                    matrix[j][i] = true;
                }
                mask[i][j] = true;
                mask[j][i] = true;
                bit++;
            }
        }
    }

    private static int getCodeOfVersion(int version) {
        int[] codes = new int[]{
                0b000010011110100110, 0b010001011100111000, 0b110111011000000100, 0b101001111110000000,
                0b001111111010111100, 0b001101100100011010, 0b101011100000100110, 0b110101000110100010,
                0b010011000010011110, 0b011100010001011100, 0b111010010101100000, 0b100100110011100100,
                0b000010110111011000, 0b000000101001111110, 0b100110101101000010, 0b111000001011000110,
                0b011110001111111010, 0b001101001101100100, 0b101011001001011000, 0b110101101111011100,
                0b010011101011100000, 0b010001110101000110, 0b110111110001111010, 0b101001010111111110,
                0b001111010011000010, 0b101000011000101101, 0b001110011100010001, 0b010000111010010101,
                0b110110111110101001, 0b110100100000001111, 0b010010100100110011, 0b001100000010110111,
                0b101010000110001011, 0b111001000100010101
        };
        return codes[version - 7];
    }

    private static void placeBlackSquare(boolean[][] matrix, boolean[][] mask) {
        int size = matrix.length;
        matrix[size - 8][8] = true;
        mask[size - 8][8] = true;
    }

    private static void reservePlaceForCodeOfMaskAndCorrectionLevel(boolean[][] mask) {
        int size = mask.length;

        for (int i = 0; i < 9; i++) {
            if (i == 6) continue;

            mask[8][i] = true;
            mask[i][8] = true;
        }
        for (int i = 0; i < 7; i++) {
            mask[size - 1 - i][8] = true;
        }
        for (int i = 0; i < 8; i++) {
            mask[8][size - 8 + i] = true;
        }
    }

    private static void fillData(int vesrion, boolean[][] matrix, boolean[][] mask, FinalBits finalBits) {
        int size = matrix.length;

        BitSet bits = finalBits.bits();
        int bitsLength = finalBits.length();

        int x = size - 1;
        int y = size - 1;
        int turnCounter = 0;
        int bitIndex = 0;
        boolean isUP = true;

        while (bitIndex < bitsLength) {
            if (isOnSyncLine(x)) {
                x--;
                continue;
            }
            if (isOnBottomSearchPattern(x, y, size)) {
                y -= 8;
                continue;
            }

            if (!mask[y][x]) {
                boolean currentBit = bits.get(bitIndex);
                matrix[y][x] = currentBit;
                bitIndex++;
            }

            if (turnCounter != 0) {
                turnCounter--;
                if (turnCounter == 0) {
                    x++;
                    isUP = !isUP;
                    y += isUP ? -1 : 1;
                } else
                    x--;
            }
            else if (isTurnCondition(x, y, size, vesrion) && bitIndex > 2) {
                turnCounter = 3;
                x--;
            }
            else {
                if (isHorizontalMove(x))
                    x--;
                else {
                    x++;
                    y += isUP ? -1 : 1;
                }
            }
        }
    }

    private static boolean isTurnCondition(int x, int y, int size, int version) {
        return (y == 0 || y == size - 1 || isNearBottomLeftSearchPattern(x, y, size) ||
                isNearMaskLevelCode(x, y, size) || isNearCodeOfVersion(x, y, size, version));
    }

    private static boolean isNearBottomLeftSearchPattern(int x, int y, int size) {
        return x <= 7 && y == size - 9;
    }

    private static boolean isNearMaskLevelCode(int x, int y, int size) {
        boolean isNearUpperLeftSearchPattern = (x <= 7 && y == 9);
        boolean isNearUpperRightSearchPattern = (x >= size - 8 && y == 9);
        return isNearUpperLeftSearchPattern || isNearUpperRightSearchPattern;
    }

    private static boolean isNearCodeOfVersion(int x, int y, int size, int version) {
        return version >= 7 && x <= 5 && y == size - 12;
    }

    private static boolean isOnSyncLine(int x) {
        return x == 6;
    }

    private static boolean isOnBottomSearchPattern(int x, int y, int size) {
        return (x == 8 && y == size - 1);
    }

    private static boolean isHorizontalMove(int x) {
        return (x % 2 == 0 && x > 5) || (x % 2 == 1 && x <= 5);
    }

    private static boolean[][] getMatrixWithBestMaskAndCorrectionLevelCode(
            boolean[][] matrix, boolean[][] mask, ServiceInfo serviceInfo) {

        int bestPenalty = Integer.MAX_VALUE;
        boolean[][] bestMatrix = null;

        for (int numOfMask = 0; numOfMask < 8; numOfMask++) {
            boolean[][] candidateMatrix = getMaskedMatrixWithMaskAndCorrectionLevelCode(
                    matrix, mask, numOfMask, serviceInfo);

            int penalty = getPenalty(candidateMatrix);

            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestMatrix = candidateMatrix;
            }
        }

        return bestMatrix;
    }

    private static boolean[][] getMaskedMatrixWithMaskAndCorrectionLevelCode(
            boolean[][] matrix, boolean[][] mask, int numOfMask, ServiceInfo serviceInfo) {

        boolean[][] maskedMatrix = applyMask(matrix, mask, numOfMask);
        placeCodeOfMaskAndCorrectionLevel(maskedMatrix, numOfMask, serviceInfo);
        return maskedMatrix;
    }

    private static boolean[][] applyMask(boolean[][] matrix, boolean[][] mask, int numOfMask) {
        int size = matrix.length;
        boolean[][] maskedMatrix = new boolean[size][size];

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!mask[y][x] && isReversedModule(x, y, numOfMask))
                    maskedMatrix[y][x] = !matrix[y][x];
                else maskedMatrix[y][x] = matrix[y][x];
            }
        }

        return maskedMatrix;
    }

    private static boolean isReversedModule(int x, int y, int numOfMask) {
        return switch (numOfMask) {
            case 0 -> (x + y) % 2 == 0;
            case 1 -> y % 2 == 0;
            case 2 -> x % 3 == 0;
            case 3 -> (x + y) % 3 == 0;
            case 4 -> (x / 3  + y / 2) % 2 == 0;
            case 5 -> (x * y) % 2 + (x * y) % 3 == 0;
            case 6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0;
            case 7 -> ((x * y) % 3 + (x + y) % 2) % 2 == 0;
            default -> throw new IllegalStateException("Unexpected value: " + numOfMask);
        };
    }

    private static void placeCodeOfMaskAndCorrectionLevel(boolean[][] matrix, int numOfMask, ServiceInfo serviceInfo) {
        int codeOfMaskAndCorrectionLevel = getCodeOfMaskAndCorrectionLevel(numOfMask, serviceInfo);
        int size = matrix.length;

        for (int i = 0; i < 9; i++) {
            if (i == 6) continue;

            int biggerThanSix = i > 6 ? 1 : 0;
            int frontBit = (codeOfMaskAndCorrectionLevel >> (14 - i + biggerThanSix)) & 1;
            int backBit = (codeOfMaskAndCorrectionLevel >> (i - biggerThanSix)) & 1;

            matrix[8][i] = (frontBit == 1);
            matrix[i][8] = (backBit == 1);
        }

        for (int i = 0; i < 9; i++) {
            int frontBit = (codeOfMaskAndCorrectionLevel >> (14 - i)) & 1;
            int backBit = (codeOfMaskAndCorrectionLevel >> i) & 1;

            if (i != 7) {
                matrix[size - 1 - i][8] = (frontBit == 1);
                matrix[8][size - 1 - i] = (backBit == 1);
            } else
                matrix[8][size - 8] = (frontBit == 1);
        }
    }

    private static int getCodeOfMaskAndCorrectionLevel(int numOfMask, ServiceInfo serviceInfo) {
        int[][] codes = new int[][]{
                {0b111011111000100, 0b111001011110011, 0b111110110101010, 0b111100010011101, 0b110011000101111,
                 0b110001100011000, 0b110110001000001, 0b110100101110110},
                {0b101010000010010, 0b101000100100101, 0b101111001111100, 0b101101101001011, 0b100010111111001,
                 0b100000011001110, 0b100111110010111, 0b100101010100000},
                {0b011010101011111, 0b011000001101000, 0b011111100110001, 0b011101000000110, 0b010010010110100,
                 0b010000110000011,	0b010111011011010, 0b010101111101101},
                {0b001011010001001, 0b001001110111110, 0b001110011100111, 0b001100111010000, 0b000011101100010,
                 0b000001001010101, 0b000110100001100, 0b000100000111011}
        };
        int levelIndex = CorrectionLevel.getIndex(serviceInfo.correctionLevel());
        return codes[levelIndex][numOfMask];
    }

    private static int getPenalty(boolean[][] matrix) {
        return countPenalty1(matrix) +
               countPenalty2(matrix) +
               countPenalty3(matrix) +
               countPenalty4(matrix);
    }

    private static int countPenalty1(boolean[][] matrix) {
        int penalty = 0;
        int size = matrix.length;

        for (boolean[] row : matrix) {
            int count = 0;
            boolean prev = row[0];
            for (int x = 0; x < size; x++) {
                boolean current = row[x];
                if (current == prev)
                    count++;
                else {
                    if (count >= 5) penalty += count - 2;
                    count = 1;
                }
            }
            if (count >= 5) penalty += count - 2;
        }

        for (int x = 0; x < size; x++) {
            int count = 0;
            boolean prev = matrix[0][x];
            for (boolean[] row : matrix) {
                boolean current = row[x];
                if (current == prev)
                    count++;
                else {
                    if (count >= 5) penalty += count - 2;
                    count = 1;
                }
            }
            if (count >= 5) penalty += count - 2;
        }

        return penalty;
    }

    private static int countPenalty2(boolean[][] matrix) {
        int penalty = 0;
        int size = matrix.length;

        for (int y = 0; y < size - 1; y++) {
            for (int x = 0; x < size - 1; x++) {
                boolean current = matrix[y][x];
                if (matrix[y][x + 1] == current &&
                    matrix[y + 1][x] == current &&
                    matrix[y + 1][x + 1] == current) {
                    penalty += 3;
                }
            }
        }

        return penalty;
    }

    private static int countPenalty3(boolean[][] matrix) {
        int penalty = 0;
        int size = matrix.length;

        int[] pattern1 = {1, 0, 1, 1, 1, 0, 1};
        int[] pattern2 = {0, 0, 0, 0};

        for (int y = 0; y < size; y++) {
            for (int x = 0; x <= size - 7; x++) {
                if (!matchesPattern(matrix, pattern1, x, y, true))
                    continue;

                if (x <= size - 11 && matchesPattern(matrix, pattern2, x + 7, y, true))
                    penalty += 40;
                else if (x >= 4 && matchesPattern(matrix, pattern2, x - 4, y, true))
                    penalty += 40;
            }
        }

        for (int x = 0; x < size; x++) {
            for (int y = 0; y <= size - 7; y++) {
                if (!matchesPattern(matrix, pattern1, x, y, false))
                    continue;

                if (y <= size - 11 && matchesPattern(matrix, pattern2, x, y + 7, false))
                    penalty += 40;
                else if (y >= 4 && matchesPattern(matrix, pattern2, x, y - 4, false))
                    penalty += 40;
            }
        }
        return penalty;
    }

    private static boolean matchesPattern(boolean[][] matrix, int[] pattern, int startX, int startY, boolean isRow) {
        int patternLength = pattern.length;
        if (isRow) {
            for (int i = 0; i < patternLength; i++) {
                if (matrix[startY][startX + i] != (pattern[i] == 1)) return false;
            }
        } else {
            for (int i = 0; i < patternLength; i++) {
                if (matrix[startY + i][startX] != (pattern[i] == 1)) return false;
            }
        }
        return true;
    }

    private static int countPenalty4(boolean[][] matrix) {
        int total = matrix.length * matrix.length;
        int dark = 0;

        for (boolean[] row : matrix) {
            for (boolean module : row) {
                if (module) dark++;
            }
        }

        int percentDark = (dark * 100) / total;
        int deviation = Math.abs(percentDark - 50);
        return (deviation / 5) * 10;
    }
}
