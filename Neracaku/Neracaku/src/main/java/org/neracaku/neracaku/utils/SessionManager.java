package org.neracaku.neracaku.utils; // PASTIKAN PACKAGE SESUAI

import org.neracaku.neracaku.models.User; // Import kelas User Anda

public class SessionManager {

    // Variabel statis untuk menyimpan instance pengguna yang sedang login.
    // 'static' berarti hanya ada satu instance variabel ini untuk seluruh aplikasi.
    private static User currentUser = null;

    /**
     * Private constructor untuk mencegah pembuatan instance dari kelas utilitas ini.
     */
    private SessionManager() {
        // Kelas utilitas tidak seharusnya diinstansiasi.
    }

    /**
     * Menetapkan pengguna yang saat ini login.
     *
     * @param user Objek User yang login.
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Mendapatkan pengguna yang saat ini login.
     *
     * @return Objek User yang sedang login, atau null jika tidak ada sesi aktif.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Menghapus sesi pengguna saat ini (logout).
     */
    public static void clearSession() {
        currentUser = null;
    }

    /**
     * Memeriksa apakah ada pengguna yang sedang login.
     *
     * @return true jika ada sesi pengguna aktif, false jika tidak.
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * (Opsional) Mendapatkan ID pengguna yang sedang login.
     * Berguna agar tidak perlu selalu memanggil getCurrentUser().getUserId().
     *
     * @return ID pengguna yang login, atau -1 (atau lempar exception) jika tidak ada sesi.
     */
    public static int getCurrentUserId() {
        if (isLoggedIn()) {
            return currentUser.getUserId();
        }
        return -1; // Atau throw new IllegalStateException("Tidak ada pengguna yang login.");
    }

    /**
     * (Opsional) Mendapatkan role pengguna yang sedang login.
     *
     * @return Role pengguna yang login (misal, "user" atau "admin"), atau null jika tidak ada sesi.
     */
    public static String getCurrentUserRole() {
        if (isLoggedIn()) {
            return currentUser.getRole();
        }
        return null; // Atau throw new IllegalStateException("Tidak ada pengguna yang login.");
    }
}