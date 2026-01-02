package com.matveevap.qrapp.records;

import com.matveevap.qrapp.enums.CorrectionLevel;
import com.matveevap.qrapp.enums.DataType;

import java.nio.charset.StandardCharsets;

public record ServiceInfo(
        String data,
        DataType dataType,
        CorrectionLevel correctionLevel,
        BitField dataLengthField,
        int version,
        int maxCapacity,
        int encodingMethod,
        int numberOfBlocks,
        int numberOfCorrectionBytes,
        int[] generatingPolynomial
) {
    public static ServiceInfo of(String data, CorrectionLevel correctionLevel) {
        DataType dataType = DataType.getTypeOfData(data);

        int levelIndex = CorrectionLevel.getIndex(correctionLevel);

        int encodedDataLength = getEncodedDataLength(data, dataType);
        int[] levelAndMaxCapacity = getCodeVersionAndMaxCapacity(encodedDataLength, levelIndex, dataType);
        int version = levelAndMaxCapacity[0];
        int maxCapacity = levelAndMaxCapacity[1];
        BitField dataLengthField = getDataLengthField(data, dataType, version, encodedDataLength);
        int encodingMethod = getEncodingMethod(dataType);
        int numberOfBlocks = getNumberOfBlocks(levelIndex, version);
        int numberOfCorrectionBytes = getNumberOfCorrectionBytes(levelIndex, version);
        int[] generatingPolynomial = getGeneratingPolynomial(numberOfCorrectionBytes);
        return new ServiceInfo(
                data,
                dataType,
                correctionLevel,
                dataLengthField,
                version,
                maxCapacity,
                encodingMethod,
                numberOfBlocks,
                numberOfCorrectionBytes,
                generatingPolynomial
        );
    }


    private static int[] getCodeVersionAndMaxCapacity(int encodedDataLength, int levelIndex, DataType dataType) throws IllegalArgumentException {
        int[][] maxCapacityTable = { //[correctionLevel][version]
                //L
                {152, 272, 440, 640, 864, 1088, 1248, 1552, 1856, 2192, 2592, 2960, 3424, 3688, 4184, 4712, 5176, 5768,
                 6360, 6888, 7456, 8048, 8752, 9392, 10208, 10960, 11744, 12248, 13048, 13880, 14744, 15640, 16568,
                 17528, 18448, 19472, 20528, 21616, 22496, 23648},

                //M
                {128, 224, 352, 512, 688, 864, 992, 1232, 1456, 1728, 2032, 2320, 2672, 2920, 3320, 3624, 4056, 4504,
                 5016, 5352, 5712, 6256, 6880, 7312, 8000, 8496, 9024, 9544, 10136, 10984, 11640, 12328, 13048, 13800,
                 14496, 15312, 15936, 16816, 17728, 18672},

                //Q
                {104, 176, 272, 384, 496, 608, 704, 880, 1056, 1232, 1440, 1648, 1952, 2088, 2360, 2600, 2936, 3176,
                 3560, 3880, 4096, 4544, 4912, 5312, 5744, 6032, 6464, 6968, 7288, 7880, 8264, 8920, 9368, 9848, 10288,
                 10832, 11408, 12016, 12656, 13328},

                //H
                {72, 128, 208, 288, 368, 480, 528, 688, 800, 976, 1120, 1264, 1440, 1576, 1784, 2024, 2264, 2504, 2728,
                 3080, 3248, 3536, 3712, 4112, 4304, 4768, 5024, 5288, 5608, 5960, 6344, 6760, 7208, 7688, 7888, 8432,
                 8768, 9136, 9776, 10208}
        };

        int maxVersion = 40;
        int lengthOfEncodeMethod = 4;
        int maxCapacity = 0;
        int version = 0;

        if (encodedDataLength > maxCapacityTable[levelIndex][maxVersion - 1])
            throw new IllegalArgumentException("Message too large or level too high. Try again.");

        for (int i = 0; i < maxVersion; i++) {
            if (encodedDataLength + getLengthOfDataLengthField(dataType, i + 1) + lengthOfEncodeMethod <= maxCapacityTable[levelIndex][i]){
                maxCapacity = maxCapacityTable[levelIndex][i];
                version = i + 1;
                break;
            }
        }
        return new int[]{version, maxCapacity};
    }

    private static int getEncodedDataLength(String data, DataType dataType) {
        int len = data.length();
        return switch (dataType) {
            case NUMERIC -> {
                int groups3 = len / 3;
                int remainder = len % 3;
                int[] bits = new int[]{0, 4, 7};
                yield groups3 * 10 + bits[remainder];
            }
            case ALPHANUMERIC -> {
                int groups2 = len / 2;
                int remainder = len % 2;
                int[] bits = new int[]{0, 6};
                yield groups2 * 11 + bits[remainder];
            }
            case BYTE -> data.getBytes(StandardCharsets.UTF_8).length * 8;
        };
    }

    private static BitField getDataLengthField(String data, DataType dataType, int version, int encodedDataLength) {
        int value = switch (dataType) {
            case NUMERIC, ALPHANUMERIC -> data.length();
            case BYTE -> encodedDataLength / 8;
        };
        int bitLength = getLengthOfDataLengthField(dataType, version);
        return new BitField(value, bitLength);
    }

    private static int getLengthOfDataLengthField(DataType dataType, int version) {
        int[][] tableOfDataLength = {
                {10, 12, 14},
                {9, 11, 13},
                {8, 16, 16}
        };
        int dataTypeIndex = switch (dataType) {
            case NUMERIC -> 0;
            case ALPHANUMERIC -> 1;
            case BYTE -> 2;
        };
        int versionIndex;
        if (version <= 9)
            versionIndex = 0;
        else if (version <= 26)
            versionIndex = 1;
        else versionIndex = 2;

        return tableOfDataLength[dataTypeIndex][versionIndex];
    }

    private static int getEncodingMethod(DataType dataType) {
        return switch (dataType){
            case BYTE -> 0b0100;
            case NUMERIC -> 0b0001;
            case ALPHANUMERIC -> 0b0010;
        };
    }

    private static int getNumberOfBlocks(int levelIndex, int version) {
        int[][] numberOfBlocksTable = new int[][] {
                {1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16,
                 17, 18, 19, 19, 20, 21, 22, 24, 25},

                {1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28,
                 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49},

                {1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35,
                 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68},

                {1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42,
                 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81}
        };
        return numberOfBlocksTable[levelIndex][version - 1];
    }

    private static int getNumberOfCorrectionBytes(int levelIndex, int version) {
        int[][] numberOfCorrectionBytesTable = new int[][]{
                {7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28,
                 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},

                {10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28,
                 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28},

                {13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28,
                 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},

                {17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30,
                 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30}
        };
        return numberOfCorrectionBytesTable[levelIndex][version - 1];
    }

    private static int[] getGeneratingPolynomial(int numberOfCorrectionBytes) throws IllegalArgumentException {
        return switch (numberOfCorrectionBytes) {
            case 7 -> new int[]{87, 229, 146, 149, 238, 102, 21};
            case 10 -> new int[]{251, 67, 46, 61, 118, 70, 64, 94, 32, 45};
            case 13 -> new int[]{74, 152, 176, 100, 86, 100, 106, 104, 130, 218, 206, 140, 78};
            case 15 -> new int[]{8, 183, 61, 91, 202, 37, 51, 58, 58, 237, 140, 124, 5, 99, 105};
            case 16 -> new int[]{120, 104, 107, 109, 102, 161, 76, 3, 91, 191, 147, 169, 182, 194, 225, 120};
            case 17 -> new int[]{43, 139, 206, 78, 43, 239, 123, 206, 214, 147, 24, 99, 150, 39, 243, 163, 136};
            case 18 -> new int[]{215, 234, 158, 94, 184, 97, 118, 170, 79, 187, 152, 148, 252, 179, 5, 98, 96, 153};
            case 20 -> new int[]{17, 60, 79, 50, 61, 163, 26, 187, 202, 180, 221, 225, 83, 239, 156, 164, 212, 212, 188,
                                 190};
            case 22 -> new int[]{210, 171, 247, 242, 93, 230, 14, 109, 221, 53, 200, 74, 8, 172, 98, 80, 219, 134, 160,
                                 105, 165, 231};
            case 24 -> new int[]{229, 121, 135, 48, 211, 117, 251, 126, 159, 180, 169, 152, 192, 226, 228, 218, 111, 0,
                                 117, 232, 87, 96, 227, 21};
            case 26 -> new int[]{173, 125, 158, 2, 103, 182, 118, 17, 145, 201, 111, 28, 165, 53, 161, 21, 245, 142, 13,
                                 102, 48, 227, 153, 145, 218, 70};
            case 28 -> new int[]{168, 223, 200, 104, 224, 234, 108, 180, 110, 190, 195, 147, 205, 27, 232, 201, 21, 43,
                                 245, 87, 42, 195, 212, 119, 242, 37, 9, 123};
            case 30 -> new int[]{41, 173, 145, 152, 216, 31, 179, 182, 50, 48, 110, 86, 239, 96, 222, 125, 42, 173, 226,
                                 193, 224, 130, 156, 37, 251, 216, 238, 40, 192, 180};
            default -> throw new IllegalArgumentException("Number of correction bytes in block must be 7 or 10 or 13 or " +
                                                   "15 or 16 or 17 or 18 or 20 or 22 or 24 or 26 or 28 or 30. Check specification");
        };
    }
}