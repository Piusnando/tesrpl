package org.neracaku.neracaku.dao; // PASTIKAN PACKAGE SESUAI

import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.utils.PasswordUtil; // Kita akan butuh ini untuk membuat admin default

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Menggunakan Optional untuk hasil pencarian yang mungkin tidak ada

public class UserDao {

    /**
     * Menyimpan pengguna baru ke database.
     * Jika pengguna berhasil disimpan, ID pengguna yang di-generate oleh database akan di-set ke objek User.
     *
     * @param user Objek User yang akan disimpan.
     * @return true jika pengguna berhasil disimpan, false jika terjadi kesalahan.
     */
    public boolean saveUser(User user) {
        String sql = "INSERT INTO users(username, password_hash, pin_hash, role) VALUES(?,?,?,?)";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getPinHash());
            pstmt.setString(4, user.getRole());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Mendapatkan ID pengguna yang di-generate
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1)); // Set ID ke objek User
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saat menyimpan pengguna: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                System.err.println("Detail: Username '" + user.getUsername() + "' kemungkinan sudah ada.");
            }
            // e.printStackTrace(); // Uncomment untuk debugging lebih detail
        }
        return false;
    }

    /**
     * Mencari pengguna berdasarkan username.
     *
     * @param username Username yang dicari.
     * @return Optional<User> yang berisi objek User jika ditemukan, atau Optional.empty() jika tidak.
     */
    public Optional<User> findUserByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash, pin_hash, role, created_at FROM users WHERE username = ?";
        User user = null;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setPinHash(rs.getString("pin_hash"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getString("created_at")); // Model User akan handle parsing String ke LocalDateTime
            }
        } catch (SQLException e) {
            System.err.println("Error saat mencari pengguna berdasarkan username: " + e.getMessage());
            // e.printStackTrace();
        }
        return Optional.ofNullable(user);
    }

    /**
     * Mencari pengguna berdasarkan ID pengguna.
     *
     * @param userId ID pengguna yang dicari.
     * @return Optional<User> yang berisi objek User jika ditemukan, atau Optional.empty() jika tidak.
     */
    public Optional<User> findUserById(int userId) {
        String sql = "SELECT user_id, username, password_hash, pin_hash, role, created_at FROM users WHERE user_id = ?";
        User user = null;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User(); // Pastikan User memiliki constructor default dan setter
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setPinHash(rs.getString("pin_hash"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getString("created_at")); // Model User akan handle parsing String ke LocalDateTime
            }
        } catch (SQLException e) {
            System.err.println("Error saat mencari pengguna berdasarkan ID: " + e.getMessage());
            // e.printStackTrace();
        }
        return Optional.ofNullable(user);
    }


    /**
     * Memeriksa apakah ada pengguna dengan role 'admin' di database.
     *
     * @return true jika ada admin, false jika tidak.
     */
    public boolean checkIfAdminExists() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'admin'";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error saat memeriksa keberadaan admin: " + e.getMessage());
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Mengambil semua pengguna dari database.
     * Digunakan oleh admin untuk melihat daftar pengguna.
     *
     * @return List dari semua objek User.
     */
    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash, pin_hash, role, created_at FROM users ORDER BY username ASC";

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash")); // Admin tidak seharusnya melihat hash secara langsung di UI
                user.setPinHash(rs.getString("pin_hash"));         // Sama seperti password_hash
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getString("created_at"));
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil semua pengguna: " + e.getMessage());
            // e.printStackTrace();
        }
        return users;
    }

    /**
     * Mengupdate data pengguna (termasuk password, PIN, dan role jika diubah oleh admin).
     *
     * @param user Objek User dengan data yang sudah diupdate.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean updateUserByAdmin(User user) {
        // Admin bisa update username (jika diizinkan & unik), password_hash, pin_hash, role
        // Kita akan buat query yang lebih fleksibel jika hanya beberapa field yang diupdate,
        // atau asumsikan semua field ini bisa diubah oleh admin.
        // Untuk kesederhanaan, kita update semua yang relevan.
        String sql = "UPDATE users SET username = ?, password_hash = ?, pin_hash = ?, role = ? WHERE user_id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getPinHash());
            pstmt.setString(4, user.getRole());
            pstmt.setInt(5, user.getUserId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error saat admin mengupdate pengguna: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                System.err.println("Detail: Username '" + user.getUsername() + "' kemungkinan sudah digunakan oleh user lain.");
            }
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Menghapus pengguna berdasarkan ID (dilakukan oleh admin).
     * HATI-HATI: Ini akan menghapus user dan semua data terkaitnya jika ada ON DELETE CASCADE di foreign key.
     *
     * @param userId ID pengguna yang akan dihapus.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean deleteUserById(int userId) {
        // Admin tidak boleh menghapus dirinya sendiri dari sini untuk mencegah terkunci
        // Validasi ini lebih baik di service layer.

        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error saat admin menghapus pengguna: " + e.getMessage());
            // e.printStackTrace();
        }
        return false;
    }


    // --- Main method untuk testing UserDao (opsional) ---
    public static void main(String[] args) {
        UserDao userDao = new UserDao();

        // Pastikan database dan tabel sudah dibuat oleh DatabaseHelper.main() sebelumnya

        // 1. Cek apakah admin sudah ada
        System.out.println("Apakah admin sudah ada? " + userDao.checkIfAdminExists());

        // 2. Coba buat admin baru jika belum ada
        if (!userDao.checkIfAdminExists()) {
            System.out.println("\nMembuat admin default...");
            User adminUser = new User();
            adminUser.setUsername("admin");
            // Gunakan PasswordUtil untuk hash password dan PIN
            adminUser.setPasswordHash(PasswordUtil.hashPassword("admin123"));
            adminUser.setPinHash(PasswordUtil.hashPin("1234")); // Contoh PIN
            adminUser.setRole("admin");

            if (userDao.saveUser(adminUser)) {
                System.out.println("Admin default berhasil dibuat dengan ID: " + adminUser.getUserId());
            } else {
                System.out.println("Gagal membuat admin default.");
            }
        } else {
            System.out.println("Admin sudah ada, tidak perlu membuat baru.");
        }

        // 3. Coba buat user biasa baru
        System.out.println("\nMembuat user biasa 'testuser'...");
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash(PasswordUtil.hashPassword("password123"));
        testUser.setPinHash(PasswordUtil.hashPin("6789"));
        testUser.setRole("user");

        if (userDao.saveUser(testUser)) {
            System.out.println("User 'testuser' berhasil dibuat dengan ID: " + testUser.getUserId());
        } else {
            System.out.println("Gagal membuat user 'testuser'. Mungkin sudah ada?");
        }

        // 4. Coba cari admin
        System.out.println("\nMencari user 'admin'...");
        Optional<User> foundAdmin = userDao.findUserByUsername("admin");
        if (foundAdmin.isPresent()) {
            System.out.println("Admin ditemukan: " + foundAdmin.get().getUsername() + " (Role: " + foundAdmin.get().getRole() + ")");
        } else {
            System.out.println("Admin tidak ditemukan.");
        }

        // 5. Coba cari user yang tidak ada
        System.out.println("\nMencari user 'penggunatidakada'...");
        Optional<User> notFoundUser = userDao.findUserByUsername("penggunatidakada");
        if (notFoundUser.isPresent()) {
            System.out.println("Pengguna ditemukan: " + notFoundUser.get());
        } else {
            System.out.println("Pengguna 'penggunatidakada' tidak ditemukan.");
        }

        // 6. Coba simpan user dengan username yang sudah ada (untuk melihat error constraint)
        System.out.println("\nMencoba menyimpan user 'admin' lagi...");
        User duplicateAdmin = new User("admin", PasswordUtil.hashPassword("newpass"), PasswordUtil.hashPin("0000"), "admin");
        if (userDao.saveUser(duplicateAdmin)) {
            System.out.println("Ini seharusnya tidak terjadi, user admin berhasil disimpan lagi.");
        } else {
            System.out.println("Gagal menyimpan user 'admin' lagi (sesuai harapan karena username harus unik).");
        }
    }
}