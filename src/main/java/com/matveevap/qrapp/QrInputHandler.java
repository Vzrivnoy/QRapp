package com.matveevap.qrapp;

import com.matveevap.qrapp.enums.CorrectionLevel;
import com.matveevap.qrapp.records.ServiceInfo;

import java.io.BufferedReader;
import java.io.IOException;

public class QrInputHandler {
    static ServiceInfo scanQrParameters(BufferedReader reader) {
        String data = null;
        CorrectionLevel level = null;
        ServiceInfo serviceInfo = null;

        while (data == null || level == null) {
            if (data == null) {
                System.out.println("Enter text for QR-code:");
                System.out.print(">>> ");
                try {
                    data = reader.readLine().trim();
                } catch (IOException e) {
                    System.out.println("Invalid input. Try again.");
                    continue;
                }
                if (data.isEmpty()) {
                    System.out.println("The text can`t be empty. Try again.");
                    data = null;
                    continue;
                }
            }

            System.out.println("""
                    List of available correction levels:
                    1. Low - maximum 7% damage available.
                    2. Medium - maximum 15% damage available.
                    3. Quartile - maximum 25% damage available.
                    4. High - maximum 30% damage available.
                    
                    Enter the number of desired correction level:
                    """);
            System.out.print(">>> ");

            try {
                String input = reader.readLine().trim();
                int lvl = Integer.parseInt(input);
                level = CorrectionLevel.fromInt(lvl);
            } catch (IOException e) {
                System.out.println("Invalid input. Try again");
                continue;
            } catch (NumberFormatException e) {
                System.out.println("Level must be an integer 1-4");
                continue;
            }

            try {
                serviceInfo = ServiceInfo.of(data, level);
            } catch (IllegalArgumentException e) {
                System.out.println(e);
                level = null;
                data = null;
            }

        }
        return serviceInfo;
    }
}
