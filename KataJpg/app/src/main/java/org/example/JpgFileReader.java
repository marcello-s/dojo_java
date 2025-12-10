/*
 * The MIT License, Copyright (c) 2011-2025 Marcel Schneider
 * for details see License.txt
 */

package org.example;
import java.io.*;

public class JpgFileReader {

    public void read(String filePath) {       
        // check file exists
        var file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + filePath);
            return;
        }

        try {
            var fis = new FileInputStream(file);
            var bis = new BufferedInputStream(fis);
            try (bis) {

                // find the SOI marker, start of image
                int soiFirstByte = getNextByte(bis) & 0xFF;
                int soiSecondByte = getNextByte(bis) & 0xFF;
                System.out.printf("SOI marker bytes: %02X %02X%n", soiFirstByte, soiSecondByte);
                if (soiFirstByte != 0xFF || soiSecondByte != 0xD8) {
                    System.out.println("Not a valid JPEG file.");
                    return;
                }
                System.out.println("JPEG Start of Image (SOI) marker found.");

                // find the APP0 marker
                int app0FirstByte = getNextByte(bis) & 0xFF;
                int app0SecondByte = getNextByte(bis) & 0xFF;
                System.out.printf("APP0 marker bytes: %02X %02X%n", app0FirstByte, app0SecondByte);
                if (app0FirstByte != 0xFF || app0SecondByte != 0xE0) {
                    System.out.println("APP0 marker not found where expected.");
                    return;
                }
                System.out.println("JPEG APP0 marker found.");

                // APP0 marker segment length
                int lengthByte1 = getNextByte(bis);
                int lengthByte2 = getNextByte(bis);
                int app0Length = ((lengthByte1 & 0xFF) << 8) | (lengthByte2 & 0xFF);
                System.out.printf("APP0 segment length: %d bytes%n", app0Length);

                // Read the identifier "JFIF\0"
                int[] identifierBytes = new int[5];
                for (int i = 0; i < 5; i++) {
                    identifierBytes[i] = getNextByte(bis);
                }
                var identifier = "";
                for (int b : identifierBytes) {
                    identifier += (char) b;
                }
                System.out.printf("APP0 Identifier: %s%n", identifier);

                // JFIF version
                int versionMajor = getNextByte(bis);
                int versionMinor = getNextByte(bis);
                System.out.printf("JFIF Version: %d.%02d%n", versionMajor, versionMinor);

                int byteRead;
                for (int i = 0; i < 5; i++) {
                    byteRead = getNextByte(bis);
                    System.out.printf("byte: %02X%n", byteRead);
                }

                bis.close();
                fis.close();
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }

        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());    
        }
    }

   private byte getNextByte(BufferedInputStream bis) throws IOException {
        return (byte) bis.read();
    }
}