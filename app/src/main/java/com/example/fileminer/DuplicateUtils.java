package com.example.fileminer;

import java.io.FileInputStream;
import java.security.MessageDigest;

public class DuplicateUtils {

    public static String getFileHash(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(filePath);

            byte[] byteArray = new byte[1024];
            int bytesRead;
            int totalRead = 0;
            int maxBytes = 1024 * 1024;

            while ((bytesRead = fis.read(byteArray)) != -1 && totalRead < maxBytes) {
                digest.update(byteArray, 0, bytesRead);
                totalRead += bytesRead;
            }
            fis.close();

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
