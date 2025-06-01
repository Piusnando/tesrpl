package org.neracaku.neracaku.services;

import org.neracaku.neracaku.dao.UserDao;
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.utils.PasswordUtil;
import org.neracaku.neracaku.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserDao userDao;

    public UserService() {
        this.userDao = new UserDao();
    }

    public boolean isUsernameTaken(String username) {
        return userDao.findUserByUsername(username).isPresent();
    }

    public boolean registerUser(String username, String plainPassword, String plainPin) {
        if (username == null || username.trim().isEmpty() ||
                plainPassword == null || plainPassword.isEmpty() ||
                plainPin == null || plainPin.isEmpty()) {
            System.err.println("Username, password, dan PIN tidak boleh kosong untuk registrasi.");
            return false; // Atau lempar exception
        }

        if (isUsernameTaken(username.trim())) {
            System.err.println("Username '" + username.trim() + "' sudah digunakan.");
            return false;
        }

        // TODO: Tambahkan validasi panjang/kompleksitas password & PIN di sini jika perlu

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPasswordHash(PasswordUtil.hashPassword(plainPassword));
        newUser.setPinHash(PasswordUtil.hashPin(plainPin));
        newUser.setRole("user"); // User baru selalu memiliki role "user"

        return userDao.saveUser(newUser);
    }

    /**
     * Mengambil semua pengguna (hanya untuk admin).
     * @return List semua User.
     */
    public List<User> getAllUsersForAdmin() {
        if (!"admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
            System.err.println("Akses ditolak: Hanya admin yang dapat melihat semua pengguna.");
            return new ArrayList<>(); // Kembalikan list kosong
        }
        return userDao.findAllUsers();
    }

    /**
     * Admin membuat pengguna baru.
     * @param username Username baru.
     * @param plainPassword Password awal.
     * @param plainPin PIN awal.
     * @param role Role pengguna baru ("user" atau "admin").
     * @return User objek jika berhasil, null jika gagal.
     */
    public User createUserByAdmin(String username, String plainPassword, String plainPin, String role) {
        if (!"admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
            System.err.println("Akses ditolak: Hanya admin yang dapat membuat pengguna.");
            return null;
        }
        if (username == null || username.trim().isEmpty() ||
                plainPassword == null || plainPassword.isEmpty() ||
                plainPin == null || plainPin.isEmpty() ||
                role == null || (!role.equals("user") && !role.equals("admin"))) {
            System.err.println("Input tidak valid untuk membuat pengguna baru.");
            return null;
        }
        if (userDao.findUserByUsername(username.trim()).isPresent()) {
            System.err.println("Username '" + username.trim() + "' sudah digunakan.");
            return null;
        }

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPasswordHash(PasswordUtil.hashPassword(plainPassword));
        newUser.setPinHash(PasswordUtil.hashPin(plainPin));
        newUser.setRole(role);

        if (userDao.saveUser(newUser)) {
            return newUser; // Kembalikan user yang baru dibuat (dengan ID)
        }
        return null;
    }

    /**
     * Admin mengupdate data pengguna.
     * @param userToUpdate Objek User dengan data yang akan diupdate.
     *                     Password dan PIN harus sudah di-hash jika diubah.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean updateUserByAdmin(User userToUpdate) {
        if (!"admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
            System.err.println("Akses ditolak: Hanya admin yang dapat mengupdate pengguna.");
            return false;
        }
        if (userToUpdate == null || userToUpdate.getUserId() == 0) {
            System.err.println("Data pengguna untuk diupdate tidak valid.");
            return false;
        }

        // Validasi jika username diubah, apakah username baru sudah dipakai user lain?
        Optional<User> existingUserWithNewName = userDao.findUserByUsername(userToUpdate.getUsername());
        if (existingUserWithNewName.isPresent() && existingUserWithNewName.get().getUserId() != userToUpdate.getUserId()) {
            System.err.println("Username '" + userToUpdate.getUsername() + "' sudah digunakan oleh user lain.");
            return false;
        }
        // Password dan PIN di field userToUpdate diasumsikan sudah di-hash jika ada perubahan.
        // Controller dialog akan bertanggung jawab untuk hashing jika password/PIN baru dimasukkan.
        return userDao.updateUserByAdmin(userToUpdate);
    }

    /**
     * Admin menghapus pengguna.
     * Admin tidak bisa menghapus dirinya sendiri.
     * @param userIdToDelete ID pengguna yang akan dihapus.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean deleteUserByAdmin(int userIdToDelete) {
        if (!"admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
            System.err.println("Akses ditolak: Hanya admin yang dapat menghapus pengguna.");
            return false;
        }
        if (SessionManager.getCurrentUserId() == userIdToDelete) {
            System.err.println("Admin tidak dapat menghapus akunnya sendiri.");
            return false;
        }
        // DAO akan menghapus. Konfirmasi lebih lanjut mungkin ada di Controller.
        return userDao.deleteUserById(userIdToDelete);
    }

    /**
     * Mendapatkan user berdasarkan ID (mungkin dibutuhkan oleh admin untuk edit).
     * @param userId ID user.
     * @return Optional<User>.
     */
    public Optional<User> getUserByIdForAdmin(int userId) {
        if (!"admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
            System.err.println("Akses ditolak.");
            return Optional.empty();
        }
        // Untuk admin, kita bisa asumsikan dia boleh lihat data user mana saja via ID
        // DAO tidak memiliki findUserById, jadi kita bisa gunakan findUserByUsername jika username unik
        // atau tambahkan findUserById di UserDao jika perlu.
        // Untuk sekarang, kita asumsikan admin akan mengedit dari daftar yang sudah ada username-nya.
        // Jika hanya punya ID, perlu findUserById di UserDao.
        // Mari kita tambahkan findUserById di UserDao.
        return userDao.findUserById(userId); // Asumsikan method ini akan dibuat di UserDao
    }
}
