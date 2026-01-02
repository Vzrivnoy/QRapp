package com.matveevap.qrapp;

import com.matveevap.qrapp.enums.DataType;
import com.matveevap.qrapp.records.BitData;
import com.matveevap.qrapp.records.BitField;
import com.matveevap.qrapp.records.FinalBits;
import com.matveevap.qrapp.records.ServiceInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class Main{

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
        byte[] finalBytes = getFinalBytes(chunksOfData, chunksOfCorrectionBytes, serviceInfo);
        FinalBits finalBits = FinalBits.of(finalBytes);

        boolean[][] qrMatrix = QRMatrix.getQRMatrix(finalBits, serviceInfo);
        QRPainter.paintQRCode(qrMatrix);
    }

    static ArrayList<BitField> getEncodedData(String data, DataType dataType) {
        ArrayList<BitField> fields = new ArrayList<>();
        switch (dataType) {
            case BYTE:
                for (byte b : data.getBytes(StandardCharsets.UTF_8))
                    fields.add(new BitField(b, 8));
                break;
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
                break;
            case ALPHANUMERIC:
                for (int i = 0; i < data.length(); i += 2) {
                    String chunk = data.substring(i, Math.min(i + 2, data.length()));
                    fields.add(new BitField(AlphaNumericCodec.encodeChunk(chunk), chunk.length() == 1 ? 6 : 11));
                }
                break;
        }
        return fields;
    }

    static BitField getServiceDataField(ServiceInfo serviceInfo) {
        int encodingMethod = serviceInfo.encodingMethod();
        int bitsOfEncodingMethod = 4;
        BitField dataLengthField = serviceInfo.dataLengthField();
        return new BitField((encodingMethod << dataLengthField.bitlength()) | dataLengthField.value(),
                            bitsOfEncodingMethod + dataLengthField.bitlength());
    }

    static byte[] getDataWithServiceFields(List<BitField> binaryData, BitField serviceFieldData, ServiceInfo serviceInfo) {
        List<BitField> allFields = new ArrayList<>(binaryData);
        allFields.addFirst(serviceFieldData);
        BitData data = BitData.of(allFields);

        byte[] buffer = data.bytes();
        int currentBits = buffer.length * 8;

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
        int dataLengthInBytes = serviceInfo.maxCapacity() / 8;
        int numberOfCorrectionBytes = 0;
        int maxCorrectionBytes = 0;

        for (byte[] correctionBytes : chunksOfCorrectionBytes) {
            numberOfCorrectionBytes += correctionBytes.length;
            maxCorrectionBytes = Math.max(maxCorrectionBytes, correctionBytes.length);
        }
        byte[] finalBytes = new byte[dataLengthInBytes + numberOfCorrectionBytes];

        int maxBytes = chunksOfData[chunksOfData.length - 1].length;
        int k = 0;
        for (int byteNumber = 1; byteNumber <= maxBytes; byteNumber++) {
            for (byte[] chunkOfData : chunksOfData) {
                if (chunkOfData.length < byteNumber)
                    continue;

                finalBytes[k] = chunkOfData[byteNumber - 1];
                k++;
            }
        }

        for (int byteNumber = 1; byteNumber <= maxCorrectionBytes; byteNumber++) {
            for (byte[] chunkOfCorrectionBytes : chunksOfCorrectionBytes) {
                if (chunkOfCorrectionBytes.length < byteNumber)
                    continue;

                finalBytes[k] = chunkOfCorrectionBytes[byteNumber - 1];
                k++;
            }
        }

        return finalBytes;
    }
}