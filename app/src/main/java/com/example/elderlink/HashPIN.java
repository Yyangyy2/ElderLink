package com.example.elderlink;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashPIN {
    public static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");  // Using SHA-256 Secure Hash Algorithm 256-bit (industry standard) for hashing
            byte[] hash = digest.digest(pin.getBytes());      // pin.getBytes() convert pin to bytes and digest.digest() hash it into byte array (  byte[] )

            // Converts the 32-byte hash into a 64-character hexadecimal string for easy storage.
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

