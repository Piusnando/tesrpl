package org.neracaku.neracaku.services; // PASTIKAN PACKAGE SESUAI

import org.neracaku.neracaku.dao.CategoryDao;
import org.neracaku.neracaku.dao.UserDao;
import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CategoryService {

    private final CategoryDao categoryDao;

    public CategoryService() {
        this.categoryDao = new CategoryDao(); // Inisialisasi DAO
    }

    /**
     * Menyimpan kategori baru.
     * Jika bukan kategori default, userId akan diambil dari SessionManager.
     *
     * @param category Objek Category dengan nama dan tipe.
     * @return true jika berhasil, false jika gagal atau nama/tipe kosong.
     */
    public boolean saveNewCategory(Category category) {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty() ||
                category.getType() == null || category.getType().trim().isEmpty()) {
            System.err.println("Nama dan tipe kategori tidak boleh kosong.");
            return false;
        }

        // Jika bukan kategori default, set userId dari pengguna yang sedang login
        if (!category.isIsDefault()) {
            if (!SessionManager.isLoggedIn()) {
                System.err.println("Tidak ada pengguna yang login untuk menyimpan kategori custom.");
                return false; // Tidak bisa menyimpan kategori custom tanpa user login
            }
            category.setUserId(SessionManager.getCurrentUserId());
        } else {
            // Untuk kategori default, pastikan userId null atau sesuai logika admin Anda
            // Jika admin yang membuat kategori default, userId bisa di-set ke admin_id atau null
            // Untuk kesederhanaan, DAO kita sudah handle userId null untuk default.
            category.setUserId(null); // Atau logika spesifik jika admin yang membuat
        }

        // TODO: Tambahkan validasi untuk mencegah duplikasi nama kategori untuk user yang sama dan tipe yang sama
        // List<Category> existingCategories = getAllCategoriesForDisplay();
        // boolean exists = existingCategories.stream()
        //    .anyMatch(c -> c.getName().equalsIgnoreCase(category.getName().trim()) &&
        //                   c.getType().equalsIgnoreCase(category.getType().trim()) &&
        //                   (c.getUserId() == null && category.getUserId() == null || c.getUserId() != null && c.getUserId().equals(category.getUserId())));
        // if (exists) {
        //    System.err.println("Kategori dengan nama dan tipe tersebut sudah ada.");
        //    return false;
        // }


        return categoryDao.saveCategory(category);
    }

    /**
     * Mengupdate kategori yang sudah ada.
     * Pengguna hanya bisa mengupdate kategori miliknya (bukan default).
     * Admin mungkin bisa mengupdate kategori default.
     *
     * @param category Objek Category dengan data yang sudah diupdate.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean updateExistingCategory(Category category) {
        if (category == null || category.getCategoryId() == 0 ||
                category.getName() == null || category.getName().trim().isEmpty() ||
                category.getType() == null || category.getType().trim().isEmpty()) {
            System.err.println("Data kategori untuk update tidak valid.");
            return false;
        }

        // Verifikasi: Pengguna biasa tidak boleh mengedit kategori default
        // atau kategori milik user lain
        Category existingCategory = categoryDao.findCategoryById(category.getCategoryId()).orElse(null);
        if (existingCategory == null) {
            System.err.println("Kategori yang akan diupdate tidak ditemukan.");
            return false;
        }

        if (existingCategory.isIsDefault() && !"admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
            System.err.println("Pengguna biasa tidak dapat mengedit kategori default.");
            return false;
        }
        if (!existingCategory.isIsDefault() && existingCategory.getUserId() != SessionManager.getCurrentUserId()) {
            System.err.println("Pengguna tidak berhak mengedit kategori ini.");
            return false;
        }

        // TODO: Cek duplikasi nama jika nama diubah

        return categoryDao.updateCategory(category);
    }

    /**
     * Menghapus kategori custom milik pengguna yang sedang login.
     *
     * @param categoryId ID kategori yang akan dihapus.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean deleteUserCategory(int categoryId) {
        if (!SessionManager.isLoggedIn()) {
            System.err.println("Tidak ada pengguna yang login untuk menghapus kategori.");
            return false;
        }
        // DAO akan melakukan pengecekan kepemilikan dan apakah itu default
        return categoryDao.deleteCategory(categoryId, SessionManager.getCurrentUserId());
    }

    /**
     * Menghapus kategori default (hanya bisa oleh admin).
     *
     * @param categoryId ID kategori default.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean deleteDefaultForAdmin(int categoryId) {
        if (!"admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
            System.err.println("Hanya admin yang dapat menghapus kategori default.");
            return false;
        }
        return categoryDao.deleteDefaultCategoryByAdmin(categoryId);
    }


    /**
     * Mendapatkan semua kategori yang relevan untuk ditampilkan kepada pengguna yang sedang login.
     * Ini termasuk kategori default sistem dan kategori custom milik pengguna tersebut.
     *
     * @return List<Category>
     */
    public List<Category> getAllCategoriesForDisplay() {
        List<Category> defaultCategories = categoryDao.getAllDefaultCategories();
        List<Category> userCategories = new ArrayList<>();

        if (SessionManager.isLoggedIn()) {
            userCategories = categoryDao.getAllCategoriesByUserId(SessionManager.getCurrentUserId());
        }

        // Gabungkan dan urutkan (misalnya berdasarkan nama)
        // Stream.concat bisa digunakan atau cukup addAll dan sort
        List<Category> allCategories = new ArrayList<>(defaultCategories);
        allCategories.addAll(userCategories);

        // Hapus duplikasi jika ada (misalnya user membuat kategori dengan nama sama seperti default,
        // meskipun DAO kita sudah ada UNIQUE constraint per user_id, name, type)
        // Untuk kasus di mana default bisa punya user_id NULL dan user membuat dengan user_id nya.
        // Untuk tampilan, mungkin kita hanya ingin satu "Gaji".
        // Ini bisa jadi lebih kompleks, untuk sekarang kita gabungkan saja.

        return allCategories.stream()
                .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Mendapatkan semua kategori berdasarkan tipe (pemasukan/pengeluaran) untuk pengguna saat ini.
     * Berguna untuk ComboBox di form transaksi.
     * @param type "pemasukan" atau "pengeluaran"
     * @return List<Category>
     */
    public List<Category> getCategoriesByTypeForCurrentUser(String type) {
        if (type == null || type.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String typeLower = type.toLowerCase();
        return getAllCategoriesForDisplay().stream()
                .filter(c -> typeLower.equals(c.getType().toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Mendapatkan kategori berdasarkan ID-nya.
     *
     * @param categoryId ID kategori yang dicari.
     * @return Optional<Category> yang berisi objek Category jika ditemukan.
     */
    public Optional<Category> getCategoryById(int categoryId) {
        // Langsung mendelegasikan ke DAO
        return categoryDao.findCategoryById(categoryId);
    }

    // --- Main method untuk testing CategoryService (opsional) ---
    public static void main(String[] args) {
        CategoryService categoryService = new CategoryService();

        // Sediakan dummy user untuk testing jika perlu
        UserDao userDao = new UserDao();
        if (!userDao.checkIfAdminExists()) {
            AuthService authSvc = new AuthService(); authSvc.initializeAdminUser();
        }
        User adminUser = userDao.findUserByUsername("admin").orElse(null);
        if (adminUser == null) {
            System.err.println("Gagal mendapatkan user admin untuk testing. Pastikan admin ada.");
            return;
        }
        SessionManager.setCurrentUser(adminUser); // Simulasikan admin login

        System.out.println("--- Testing Category Service ---");

        // 1. Inisialisasi beberapa kategori default jika belum ada (melalui DAO langsung atau service)
        // Kita asumsikan "Gaji" sudah ada dari test DAO
        Category transportDefault = new Category("Transportasi (Default)", "pengeluaran");
        transportDefault.setIsDefault(true);
        categoryService.saveNewCategory(transportDefault); // Seharusnya berhasil jika belum ada

        // 2. Simpan kategori baru untuk user saat ini
        System.out.println("\nMenyimpan kategori baru 'Belanja Harian'...");
        Category belanja = new Category("Belanja Harian", "pengeluaran");
        // isDefault false secara otomatis, userId akan di-set oleh service
        if (categoryService.saveNewCategory(belanja)) {
            System.out.println("Berhasil disimpan: " + belanja.getName() + " (ID: " + belanja.getCategoryId() + ")");
        } else {
            System.out.println("Gagal menyimpan 'Belanja Harian'.");
        }

        // 3. Coba simpan kategori duplikat (jika validasi duplikasi diaktifkan)
        // System.out.println("\nMencoba menyimpan 'Belanja Harian' lagi...");
        // Category belanjaDuplikat = new Category("Belanja Harian", "pengeluaran");
        // if (!categoryService.saveNewCategory(belanjaDuplikat)) {
        //     System.out.println("Gagal menyimpan duplikat (sesuai harapan jika validasi ada).");
        // }

        // 4. Tampilkan semua kategori untuk user saat ini
        System.out.println("\nSemua Kategori untuk user '" + SessionManager.getCurrentUser().getUsername() + "':");
        List<Category> allCats = categoryService.getAllCategoriesForDisplay();
        allCats.forEach(c -> System.out.println("- " + c.getName() + " (Tipe: " + c.getType() + ", Default: " + c.isIsDefault() + ", UserID: " + c.getUserId() + ")"));

        // 5. Tampilkan kategori pemasukan
        System.out.println("\nKategori Pemasukan:");
        categoryService.getCategoriesByTypeForCurrentUser("pemasukan")
                .forEach(c -> System.out.println("- " + c.getName()));

        // 6. Update kategori
        if (!allCats.isEmpty()) {
            Category catToUpdate = allCats.stream().filter(c -> "Belanja Harian".equals(c.getName())).findFirst().orElse(null);
            if (catToUpdate != null && !catToUpdate.isIsDefault()) { // Hanya update kategori custom
                System.out.println("\nMengupdate '" + catToUpdate.getName() + "' menjadi 'Belanja Bulanan'...");
                catToUpdate.setName("Belanja Bulanan");
                if (categoryService.updateExistingCategory(catToUpdate)) {
                    System.out.println("Berhasil diupdate.");
                } else {
                    System.out.println("Gagal mengupdate.");
                }
            }
        }

        // 7. Hapus kategori
        List<Category> catsAfterUpdate = categoryService.getAllCategoriesForDisplay();
        Category catToDelete = catsAfterUpdate.stream().filter(c -> "Belanja Bulanan".equals(c.getName())).findFirst().orElse(null);
        if (catToDelete != null && !catToDelete.isIsDefault()) {
            System.out.println("\nMenghapus kategori '" + catToDelete.getName() + "'...");
            if (categoryService.deleteUserCategory(catToDelete.getCategoryId())) {
                System.out.println("Berhasil dihapus.");
            } else {
                System.out.println("Gagal dihapus.");
            }
        }

        System.out.println("\nKategori setelah operasi:");
        categoryService.getAllCategoriesForDisplay().forEach(c -> System.out.println("- " + c.getName()));


        SessionManager.clearSession();
        System.out.println("\n--- Testing Category Service Selesai ---");
    }
}