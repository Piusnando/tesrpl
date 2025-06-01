package org.neracaku.neracaku.services; // PASTIKAN PACKAGE SESUAI

import org.neracaku.neracaku.dao.UserDao;
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.utils.PasswordUtil;

import java.util.Optional;

public class AuthService {

    private final UserDao userDao; // Dependensi ke UserDao

    // Constructor untuk injecting UserDao (Dependency Injection sederhana)
    public AuthService() {
        this.userDao = new UserDao(); // Membuat instance UserDao langsung
        // Dalam aplikasi yang lebih besar, Anda mungkin menggunakan framework DI
    }

    // Constructor alternatif jika Anda ingin menyediakan instance UserDao dari luar
    // public AuthService(UserDao userDao) {
    //     this.userDao = userDao;
    // }

    /**
     * Menginisialisasi pengguna admin default jika belum ada di database.
     * Metode ini sebaiknya dipanggil saat aplikasi pertama kali startup.
     */
    public void initializeAdminUser() {
        if (!userDao.checkIfAdminExists()) {
            System.out.println("Admin default tidak ditemukan. Membuat admin default...");
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(PasswordUtil.hashPassword("admin123")); // Password default
            admin.setPinHash(PasswordUtil.hashPin("1234")); // PIN default
            admin.setRole("admin");

            if (userDao.saveUser(admin)) {
                System.out.println("Admin default berhasil dibuat dengan username 'admin'.");
            } else {
                System.err.println("Gagal membuat admin default. Periksa log error UserDao.");
                // Anda mungkin ingin melempar exception di sini jika kritis
            }
        } else {
            System.out.println("Admin default sudah ada.");
        }
    }

    /**
     * Melakukan proses login pengguna.
     *
     * @param username Username yang dimasukkan.
     * @param plainPassword Password plain text yang dimasukkan.
     * @return Optional<User> yang berisi objek User jika login berhasil, atau Optional.empty() jika gagal.
     */
    public Optional<User> login(String username, String plainPassword) {
        if (username == null || username.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            System.err.println("Username atau password tidak boleh kosong.");
            return Optional.empty(); // Atau lempar IllegalArgumentException
        }

        Optional<User> userOptional = userDao.findUserByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Verifikasi password
            if (PasswordUtil.checkPassword(plainPassword, user.getPasswordHash())) {
                return Optional.of(user); // Login berhasil
            } else {
                System.out.println("Password salah untuk user: " + username);
            }
        } else {
            System.out.println("User tidak ditemukan: " + username);
        }
        return Optional.empty(); // Login gagal
    }

    /**
     * Memverifikasi PIN pengguna.
     *
     * @param user         Objek User yang PIN-nya akan diverifikasi.
     * @param plainPin     PIN plain text yang dimasukkan.
     * @return true jika PIN cocok, false jika tidak.
     */
    public boolean verifyPin(User user, String plainPin) {
        if (user == null || plainPin == null || plainPin.isEmpty()) {
            System.err.println("User atau PIN tidak boleh kosong untuk verifikasi.");
            return false; // Atau lempar IllegalArgumentException
        }
        // Pastikan user memiliki PIN hash (meskipun seharusnya selalu ada jika user valid)
        if (user.getPinHash() == null || user.getPinHash().isEmpty()){
            System.err.println("User tidak memiliki PIN yang tersimpan.");
            return false;
        }

        return PasswordUtil.checkPin(plainPin, user.getPinHash());
    }

    // --- Main method untuk testing AuthService (opsional) ---
    public static void main(String[] args) {
        // Pastikan DatabaseHelper.main() sudah dijalankan sebelumnya untuk membuat DB & tabel.
        // Atau UserDao.main() untuk memastikan admin sudah ada (atau akan dibuat di sini).

        AuthService authService = new AuthService();

        // 1. Inisialisasi admin (akan membuat jika belum ada, atau skip jika sudah)
        System.out.println("--- Menginisialisasi Admin ---");
        authService.initializeAdminUser();
        System.out.println("------------------------------\n");


        // 2. Test login dengan kredensial admin yang benar
        System.out.println("--- Test Login Admin (Benar) ---");
        Optional<User> adminLogin = authService.login("admin", "admin123");
        if (adminLogin.isPresent()) {
            User loggedInAdmin = adminLogin.get();
            System.out.println("Login admin berhasil: " + loggedInAdmin.getUsername() + " (Role: " + loggedInAdmin.getRole() + ")");

            // Test verifikasi PIN admin yang benar
            System.out.println("Verifikasi PIN admin '1234': " + authService.verifyPin(loggedInAdmin, "1234"));
            // Test verifikasi PIN admin yang salah
            System.out.println("Verifikasi PIN admin '0000': " + authService.verifyPin(loggedInAdmin, "0000"));
        } else {
            System.out.println("Login admin gagal.");
        }
        System.out.println("------------------------------\n");


        // 3. Test login dengan username salah
        System.out.println("--- Test Login (Username Salah) ---");
        Optional<User> wrongUserLogin = authService.login("adminSalah", "admin123");
        if (wrongUserLogin.isPresent()) {
            System.out.println("Login berhasil (seharusnya tidak): " + wrongUserLogin.get().getUsername());
        } else {
            System.out.println("Login gagal karena username salah (sesuai harapan).");
        }
        System.out.println("------------------------------\n");


        // 4. Test login dengan password salah
        System.out.println("--- Test Login (Password Salah) ---");
        Optional<User> wrongPasswordLogin = authService.login("admin", "passwordSalah");
        if (wrongPasswordLogin.isPresent()) {
            System.out.println("Login berhasil (seharusnya tidak): " + wrongPasswordLogin.get().getUsername());
        } else {
            System.out.println("Login gagal karena password salah (sesuai harapan).");
        }
        System.out.println("------------------------------\n");

        // Anda bisa menambahkan test case lain jika perlu
    }
}