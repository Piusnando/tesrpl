package org.neracaku.neracaku.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Menghasilkan salt baru setiap kali, meningkatkan keamanan
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password tidak boleh kosong.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)); // Angka 12 adalah work factor (cost)
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.isEmpty() ||
                hashedPassword == null || hashedPassword.isEmpty()) {
            return false; // Password kosong atau hash kosong tidak valid
        }
        // BCrypt.checkpw akan menangani jika hashedPassword bukan format bcrypt
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Ini bisa terjadi jika hashedPassword bukan format bcrypt yang valid
            System.err.println("Error saat memeriksa password (kemungkinan format hash tidak valid): " + e.getMessage());
            return false;
        }
    }

    // Untuk PIN, kita bisa menggunakan metode yang sama
    // Jika ada kebutuhan kekuatan hashing yang berbeda untuk PIN, bisa disesuaikan
    public static String hashPin(String plainPin) {
        if (plainPin == null || plainPin.isEmpty()) {
            throw new IllegalArgumentException("PIN tidak boleh kosong.");
        }
        // Bisa gunakan work factor yang sama atau berbeda jika diinginkan
        return BCrypt.hashpw(plainPin, BCrypt.gensalt(10));
    }

    public static boolean checkPin(String plainPin, String hashedPin) {
        if (plainPin == null || plainPin.isEmpty() ||
                hashedPin == null || hashedPin.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPin, hashedPin);
        } catch (IllegalArgumentException e) {
            System.err.println("Error saat memeriksa PIN (kemungkinan format hash tidak valid): " + e.getMessage());
            return false;
        }
    }

    // Method main untuk testing hashing (opsional)
    public static void main(String[] args) {
        String password = "admin123";
        String hashed = hashPassword(password);
        System.out.println("Password Asli: " + password);
        System.out.println("Password Hash: " + hashed);
        System.out.println("Verifikasi (benar): " + checkPassword(password, hashed));
        System.out.println("Verifikasi (salah): " + checkPassword("salah123", hashed));

        String pin = "123456";
        String hashedPin = hashPin(pin);
        System.out.println("\nPIN Asli: " + pin);
        System.out.println("PIN Hash: " + hashedPin);
        System.out.println("Verifikasi PIN (benar): " + checkPin(pin, hashedPin));
    }
}