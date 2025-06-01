package org.neracaku.neracaku.dao; // PASTIKAN PACKAGE SESUAI

import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.services.AuthService;
import org.neracaku.neracaku.utils.SessionManager; // Untuk mendapatkan user ID jika diperlukan

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDao {

    /**
     * Menyimpan kategori baru ke database.
     *
     * @param category Objek Category yang akan disimpan.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean saveCategory(Category category) {
        // Jika user_id null (kategori default oleh admin), kita perlu menangani PreparedStatement secara berbeda
        String sql;
        if (category.getUserId() == null) { // Kategori default
            sql = "INSERT INTO categories(name, type, is_default) VALUES(?,?,?)";
        } else { // Kategori milik pengguna
            sql = "INSERT INTO categories(user_id, name, type, is_default) VALUES(?,?,?,?)";
        }


        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (category.getUserId() != null) {
                pstmt.setInt(paramIndex++, category.getUserId());
            }
            pstmt.setString(paramIndex++, category.getName());
            pstmt.setString(paramIndex++, category.getType().toLowerCase()); // Simpan type sebagai lowercase
            pstmt.setBoolean(paramIndex, category.isIsDefault());


            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        category.setCategoryId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saat menyimpan kategori: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                System.err.println("Detail: Kombinasi nama, tipe, dan user_id kategori mungkin sudah ada.");
            }
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Mengupdate kategori yang sudah ada.
     *
     * @param category Objek Category dengan data yang sudah diupdate.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean updateCategory(Category category) {
        String sql = "UPDATE categories SET name = ?, type = ? WHERE category_id = ?";
        // Kategori default biasanya tidak diupdate oleh user biasa, mungkin hanya nama oleh admin.
        // Untuk kategori user, mereka bisa update nama dan tipe.
        // is_default dan user_id biasanya tidak diubah setelah pembuatan.

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getType().toLowerCase());
            pstmt.setInt(3, category.getCategoryId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error saat mengupdate kategori: " + e.getMessage());
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Menghapus kategori berdasarkan ID.
     * Hanya kategori custom milik pengguna yang boleh dihapus oleh pengguna tersebut.
     * Kategori default tidak boleh dihapus oleh pengguna biasa.
     *
     * @param categoryId ID kategori yang akan dihapus.
     * @param userId ID pengguna yang melakukan aksi (untuk verifikasi kepemilikan).
     * @return true jika berhasil, false jika gagal.
     */
    public boolean deleteCategory(int categoryId, int userId) {
        // Cek dulu apakah kategori ini default atau bukan milik user ini
        Optional<Category> categoryOpt = findCategoryById(categoryId);
        if (categoryOpt.isPresent()) {
            Category cat = categoryOpt.get();
            if (cat.isIsDefault()) {
                System.err.println("Tidak dapat menghapus kategori default sistem.");
                return false; // Kategori default tidak boleh dihapus user
            }
            if (cat.getUserId() == null || cat.getUserId() != userId) {
                System.err.println("Pengguna tidak memiliki hak untuk menghapus kategori ini.");
                return false; // Bukan pemilik kategori
            }
        } else {
            System.err.println("Kategori dengan ID " + categoryId + " tidak ditemukan untuk dihapus.");
            return false;
        }


        String sql = "DELETE FROM categories WHERE category_id = ? AND user_id = ? AND is_default = 0";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);
            pstmt.setInt(2, userId); // Pastikan user hanya bisa hapus miliknya

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error saat menghapus kategori: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("SQLITE_CONSTRAINT_FOREIGNKEY")) {
                System.err.println("Detail: Kategori ini mungkin masih digunakan oleh transaksi.");
            }
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Menghapus kategori default oleh admin.
     *
     * @param categoryId ID kategori default yang akan dihapus.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean deleteDefaultCategoryByAdmin(int categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ? AND is_default = 1";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error saat admin menghapus kategori default: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("SQLITE_CONSTRAINT_FOREIGNKEY")) {
                System.err.println("Detail: Kategori default ini mungkin masih digunakan oleh transaksi.");
            }
        }
        return false;
    }


    /**
     * Mencari kategori berdasarkan ID.
     *
     * @param categoryId ID kategori.
     * @return Optional<Category>
     */
    public Optional<Category> findCategoryById(int categoryId) {
        String sql = "SELECT category_id, user_id, name, type, is_default FROM categories WHERE category_id = ?";
        Category category = null;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                category = mapRowToCategory(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error mencari kategori by ID: " + e.getMessage());
        }
        return Optional.ofNullable(category);
    }

    /**
     * Mengambil semua kategori custom milik pengguna tertentu.
     *
     * @param userId ID pengguna.
     * @return List<Category>
     */
    public List<Category> getAllCategoriesByUserId(int userId) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT category_id, user_id, name, type, is_default FROM categories WHERE user_id = ? AND is_default = 0 ORDER BY name ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                categories.add(mapRowToCategory(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error mengambil kategori by user ID: " + e.getMessage());
        }
        return categories;
    }

    /**
     * Mengambil semua kategori default sistem.
     *
     * @return List<Category>
     */
    public List<Category> getAllDefaultCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT category_id, user_id, name, type, is_default FROM categories WHERE is_default = 1 ORDER BY name ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(mapRowToCategory(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error mengambil kategori default: " + e.getMessage());
        }
        return categories;
    }

    /**
     * Helper method untuk memetakan baris ResultSet ke objek Category.
     */
    private Category mapRowToCategory(ResultSet rs) throws SQLException {
        int categoryId = rs.getInt("category_id");
        // Handle user_id yang bisa NULL
        Integer userId = rs.getObject("user_id") == null ? null : rs.getInt("user_id");
        String name = rs.getString("name");
        String type = rs.getString("type");
        boolean isDefault = rs.getBoolean("is_default");
        return new Category(categoryId, name, type, isDefault, userId);
    }


    // --- Main method untuk testing CategoryDao (opsional) ---
    public static void main(String[] args) {
        CategoryDao categoryDao = new CategoryDao();
        // Pastikan user admin sudah ada (misal, ID 1) untuk testing saveCategory dengan userId
        // Atau jalankan AuthService.initializeAdminUser() atau UserDao.main() dulu

        // Simulasikan user login (untuk kategori custom)
        // User dummyUser = new User();
        // dummyUser.setUserId(1); // Asumsi ID admin = 1
        // SessionManager.setCurrentUser(dummyUser); // Ini hanya untuk testing di sini

        System.out.println("--- Testing Category DAO ---");

        // 1. Buat Kategori Default (jika belum ada)
        System.out.println("\nMembuat kategori default 'Gaji'...");
        Category gaji = new Category("Gaji", "pemasukan");
        gaji.setIsDefault(true); // Tandai sebagai default
        // Untuk kategori default, userId bisa null atau ID admin khusus jika ada
        // Jika saveCategory menangani userId null untuk default, kita bisa set:
        // gaji.setUserId(null);
        if (categoryDao.saveCategory(gaji)) { // Pastikan saveCategory bisa handle userId null untuk default
            System.out.println("Kategori default 'Gaji' berhasil disimpan dengan ID: " + gaji.getCategoryId());
        } else {
            System.out.println("Gagal menyimpan kategori default 'Gaji'. Mungkin sudah ada?");
        }

        // 2. Buat Kategori Custom untuk User (asumsi user dengan ID 1 sudah login)
        if (SessionManager.isLoggedIn() && SessionManager.getCurrentUserId() != -1) { // Cek jika ada user login
            int currentUserId = SessionManager.getCurrentUserId();
            System.out.println("\nMembuat kategori custom 'Makanan' untuk user ID: " + currentUserId);
            Category makanan = new Category("Makanan", "pengeluaran");
            makanan.setUserId(currentUserId); // Set ID user pemilik
            makanan.setIsDefault(false);
            if (categoryDao.saveCategory(makanan)) {
                System.out.println("Kategori 'Makanan' berhasil disimpan dengan ID: " + makanan.getCategoryId());
            } else {
                System.out.println("Gagal menyimpan kategori 'Makanan'.");
            }
        } else {
            System.out.println("\nTidak ada user login untuk testing kategori custom. Silakan login atau set dummy user di SessionManager.");
            // Jika ingin tetap test, buat user admin dulu jika belum
            UserDao userDao = new UserDao();
            if (!userDao.checkIfAdminExists()) {
                AuthService authSvc = new AuthService(); authSvc.initializeAdminUser();
            }
            User adminForTest = userDao.findUserByUsername("admin").orElse(null);
            if (adminForTest != null) {
                SessionManager.setCurrentUser(adminForTest);
                System.out.println("User admin diset untuk testing kategori custom.");
                // Ulangi pembuatan kategori custom makanan di sini jika perlu
                int currentUserId = SessionManager.getCurrentUserId();
                System.out.println("\nMembuat kategori custom 'Makanan' untuk user ID: " + currentUserId);
                Category makanan = new Category("Makanan", "pengeluaran");
                makanan.setUserId(currentUserId); // Set ID user pemilik
                makanan.setIsDefault(false);
                if (categoryDao.saveCategory(makanan)) {
                    System.out.println("Kategori 'Makanan' berhasil disimpan dengan ID: " + makanan.getCategoryId());
                } else {
                    System.out.println("Gagal menyimpan kategori 'Makanan'.");
                }
            }
        }


        // 3. Tampilkan Semua Kategori Default
        System.out.println("\nKategori Default Sistem:");
        List<Category> defaults = categoryDao.getAllDefaultCategories();
        defaults.forEach(c -> System.out.println("- " + c.getName() + " (" + c.getType() + ")"));

        // 4. Tampilkan Kategori Custom Milik User (jika user sudah login)
        if (SessionManager.isLoggedIn() && SessionManager.getCurrentUserId() != -1) {
            int currentUserId = SessionManager.getCurrentUserId();
            System.out.println("\nKategori Custom Milik User ID " + currentUserId + ":");
            List<Category> userCats = categoryDao.getAllCategoriesByUserId(currentUserId);
            userCats.forEach(c -> System.out.println("- " + c.getName() + " (" + c.getType() + ")"));

            // 5. Coba Update Kategori Custom
            if (!userCats.isEmpty()) {
                Category catToUpdate = userCats.get(0); // Ambil kategori pertama untuk diupdate
                System.out.println("\nMengupdate kategori '" + catToUpdate.getName() + "' menjadi 'Belanja Makanan'...");
                String originalName = catToUpdate.getName();
                catToUpdate.setName("Belanja Makanan");
                if (categoryDao.updateCategory(catToUpdate)) {
                    System.out.println("Berhasil diupdate.");
                } else {
                    System.out.println("Gagal diupdate.");
                    catToUpdate.setName(originalName); // Kembalikan nama jika gagal
                }
            }

            // 6. Coba Hapus Kategori Custom
            // Cari lagi kategori 'Belanja Makanan' atau kategori lain yang baru dibuat untuk dihapus
            List<Category> updatedUserCats = categoryDao.getAllCategoriesByUserId(currentUserId);
            Optional<Category> catToDeleteOpt = updatedUserCats.stream()
                    .filter(c -> "Belanja Makanan".equals(c.getName()) || "Makanan".equals(c.getName()))
                    .findFirst();

            if (catToDeleteOpt.isPresent()) {
                Category catToDelete = catToDeleteOpt.get();
                System.out.println("\nMencoba menghapus kategori '" + catToDelete.getName() + "'...");
                if (categoryDao.deleteCategory(catToDelete.getCategoryId(), currentUserId)) {
                    System.out.println("Berhasil dihapus.");
                } else {
                    System.out.println("Gagal dihapus.");
                }
            }
        }
        SessionManager.clearSession(); // Bersihkan sesi setelah testing
        System.out.println("\n--- Testing Category DAO Selesai ---");
    }
}