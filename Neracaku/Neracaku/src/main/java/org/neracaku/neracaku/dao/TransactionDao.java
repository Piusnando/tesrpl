package org.neracaku.neracaku.dao; // PASTIKAN PACKAGE SESUAI

import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.models.MonthlySummary;
import org.neracaku.neracaku.models.Transaction;
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.services.AuthService;
import org.neracaku.neracaku.services.CategoryService;
import org.neracaku.neracaku.utils.SessionManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionDao {

    /**
     * Menyimpan transaksi baru ke database.
     *
     * @param transaction Objek Transaction yang akan disimpan.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean saveTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions(user_id, category_id, amount, transaction_date, description, image_path, type) " +
                "VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, transaction.getUserId());
            pstmt.setInt(2, transaction.getCategoryId());
            pstmt.setDouble(3, transaction.getAmount());
            pstmt.setString(4, transaction.getTransactionDate().format(Transaction.SQLITE_DATE_FORMATTER));
            pstmt.setString(5, transaction.getDescription());
            pstmt.setString(6, transaction.getImagePath()); // Bisa null
            pstmt.setString(7, transaction.getType().toLowerCase());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transaction.setTransactionId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saat menyimpan transaksi: " + e.getMessage());
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Mengupdate transaksi yang sudah ada.
     *
     * @param transaction Objek Transaction dengan data yang sudah diupdate.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean updateTransaction(Transaction transaction) {
        String sql = "UPDATE transactions SET category_id = ?, amount = ?, transaction_date = ?, " +
                "description = ?, image_path = ?, type = ? " + // updated_at akan dihandle oleh trigger
                "WHERE transaction_id = ? AND user_id = ?"; // Pastikan user hanya update miliknya

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, transaction.getCategoryId());
            pstmt.setDouble(2, transaction.getAmount());
            pstmt.setString(3, transaction.getTransactionDate().format(Transaction.SQLITE_DATE_FORMATTER));
            pstmt.setString(4, transaction.getDescription());
            pstmt.setString(5, transaction.getImagePath());
            pstmt.setString(6, transaction.getType().toLowerCase());
            pstmt.setInt(7, transaction.getTransactionId());
            pstmt.setInt(8, transaction.getUserId()); // Verifikasi kepemilikan

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error saat mengupdate transaksi: " + e.getMessage());
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Menghapus transaksi berdasarkan ID dan User ID.
     *
     * @param transactionId ID transaksi yang akan dihapus.
     * @param userId ID pengguna pemilik transaksi.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean deleteTransaction(int transactionId, int userId) {
        String sql = "DELETE FROM transactions WHERE transaction_id = ? AND user_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, transactionId);
            pstmt.setInt(2, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error saat menghapus transaksi: " + e.getMessage());
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Mencari transaksi berdasarkan ID.
     *
     * @param transactionId ID transaksi.
     * @return Optional<Transaction>
     */
    public Optional<Transaction> findTransactionById(int transactionId) {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
        Transaction transaction = null;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, transactionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                transaction = mapRowToTransaction(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error mencari transaksi by ID: " + e.getMessage());
        }
        return Optional.ofNullable(transaction);
    }

    /**
     * Mengambil semua transaksi milik pengguna dalam rentang tanggal tertentu,
     * dengan filter kategori dan keyword opsional.
     * Hasil diurutkan berdasarkan tanggal transaksi terbaru.
     *
     * @param userId ID pengguna.
     * @param startDate Tanggal mulai (inklusif), bisa null.
     * @param endDate Tanggal akhir (inklusif), bisa null.
     * @param categoryId ID kategori untuk filter, bisa null.
     * @param keyword Kata kunci untuk pencarian di deskripsi, bisa null atau kosong.
     * @return List<Transaction>
     */
    public List<Transaction> getAllTransactionsFiltered(int userId, LocalDate startDate, LocalDate endDate,
                                                        Integer categoryId, String keyword) {
        List<Transaction> transactions = new ArrayList<>();
        // Bangun query SQL secara dinamis berdasarkan filter yang ada
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM transactions WHERE user_id = ? ");

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (startDate != null) {
            sqlBuilder.append("AND transaction_date >= ? ");
            params.add(startDate.format(Transaction.SQLITE_DATE_FORMATTER));
        }
        if (endDate != null) {
            // Batasan 12 bulan bisa diterapkan di service layer sebelum memanggil DAO,
            // atau pastikan endDate tidak lebih dari 12 bulan dari startDate.
            // Untuk query, kita ambil semua dalam rentang yang diberikan.
            sqlBuilder.append("AND transaction_date <= ? ");
            params.add(endDate.format(Transaction.SQLITE_DATE_FORMATTER));
        }
        if (categoryId != null && categoryId > 0) {
            sqlBuilder.append("AND category_id = ? ");
            params.add(categoryId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sqlBuilder.append("AND description LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }

        sqlBuilder.append("ORDER BY transaction_date DESC, created_at DESC"); // Terbaru dulu

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                transactions.add(mapRowToTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error mengambil transaksi terfilter: " + e.getMessage());
            // e.printStackTrace();
        }
        return transactions;
    }

    /**
     * Mengambil semua transaksi pengeluaran untuk pengguna tertentu dalam satu bulan.
     * @param userId ID pengguna.
     * @param year Tahun.
     * @param month Bulan (1-12).
     * @return List<Transaction>
     */
    public List<Transaction> getMonthlyExpenseTransactions(int userId, int year, int month) {
        List<Transaction> transactions = new ArrayList<>();
        // Membuat tanggal awal dan akhir bulan
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        String sql = "SELECT * FROM transactions " +
                "WHERE user_id = ? AND type = 'pengeluaran' " +
                "AND transaction_date BETWEEN ? AND ? " +
                "ORDER BY transaction_date DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, startDate.format(Transaction.SQLITE_DATE_FORMATTER));
            pstmt.setString(3, endDate.format(Transaction.SQLITE_DATE_FORMATTER));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                transactions.add(mapRowToTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error mengambil transaksi pengeluaran bulanan: " + e.getMessage());
        }
        return transactions;
    }

    public List<MonthlySummary> getMonthlySummaries(int userId, int numberOfMonths) {
        List<MonthlySummary> summaries = new ArrayList<>();
        // Ambil data untuk numberOfMonths terakhir, termasuk bulan ini
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = LocalDate.now().minusMonths(numberOfMonths - 1).withDayOfMonth(1);

        // Query untuk mengagregasi pemasukan dan pengeluaran per bulan
        // strftime('%Y-%m', transaction_date) akan menghasilkan format 'YYYY-MM'
        String sql = "SELECT " +
                "  strftime('%Y-%m', transaction_date) as month_year, " +
                "  SUM(CASE WHEN type = 'pemasukan' THEN amount ELSE 0 END) as total_income, " +
                "  SUM(CASE WHEN type = 'pengeluaran' THEN amount ELSE 0 END) as total_expense " +
                "FROM transactions " +
                "WHERE user_id = ? AND transaction_date BETWEEN ? AND ? " +
                "GROUP BY month_year " +
                "ORDER BY month_year ASC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, startDate.format(Transaction.SQLITE_DATE_FORMATTER));
            pstmt.setString(3, endDate.format(Transaction.SQLITE_DATE_FORMATTER));

            ResultSet rs = pstmt.executeQuery();
            DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

            while (rs.next()) {
                String monthYearStr = rs.getString("month_year");
                double totalIncome = rs.getDouble("total_income");
                double totalExpense = rs.getDouble("total_expense");
                System.out.println("DAO - Bulan: " + monthYearStr + ", Pemasukan: " + totalIncome + ", Pengeluaran: " + totalExpense); // DEBUG
                YearMonth ym = YearMonth.parse(monthYearStr, ymFormatter);
                summaries.add(new MonthlySummary(ym, totalIncome, totalExpense));
            }
        } catch (SQLException e) {
            System.err.println("Error mengambil ringkasan bulanan: " + e.getMessage());
            // e.printStackTrace();
        }
        return summaries;
    }


    /**
     * Helper method untuk memetakan baris ResultSet ke objek Transaction.
     */
    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(rs.getInt("transaction_id"));
        transaction.setUserId(rs.getInt("user_id"));
        transaction.setCategoryId(rs.getInt("category_id"));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setTransactionDate(rs.getString("transaction_date")); // Menggunakan setter yang parsing String
        transaction.setDescription(rs.getString("description"));
        transaction.setImagePath(rs.getString("image_path"));
        transaction.setType(rs.getString("type"));
        transaction.setCreatedAt(rs.getString("created_at")); // Menggunakan setter yang parsing String
        transaction.setUpdatedAt(rs.getString("updated_at")); // Menggunakan setter yang parsing String
        return transaction;
    }

    public static void main(String[] args) {
        TransactionDao transactionDao = new TransactionDao();
        UserDao userDao = new UserDao(); // Untuk mendapatkan user
        CategoryDao categoryDao = new CategoryDao(); // Untuk mendapatkan kategori

        // Pastikan DatabaseHelper.createTablesIfNotExists(); sudah dipanggil setidaknya sekali
        // DatabaseHelper.createTablesIfNotExists(); // Biasanya sudah di MainApp atau DAO lain

        System.out.println("--- Testing Transaction DAO ---");

        // Sediakan dummy user dan kategori untuk testing
        // Pastikan user ini ada di DB (dibuat oleh UserDao.main() atau AuthService)
        Optional<User> adminOpt = userDao.findUserByUsername("admin");
        if (adminOpt.isEmpty()) {
            System.err.println("User 'admin' tidak ditemukan. Harap buat user admin terlebih dahulu.");
            AuthService authService = new AuthService(); // Buat admin jika tidak ada
            authService.initializeAdminUser();
            adminOpt = userDao.findUserByUsername("admin");
            if(adminOpt.isEmpty()) {
                System.err.println("Masih gagal membuat/menemukan user admin. Testing dibatalkan.");
                return;
            }
        }
        User testUser = adminOpt.get();
        int testUserId = testUser.getUserId();
        SessionManager.setCurrentUser(testUser); // Simulasikan user login untuk CategoryService

        // Pastikan kategori ini ada (dibuat oleh CategoryDao.main() atau CategoryService)
        CategoryService categoryService = new CategoryService();
        List<Category> pemasukanCategories = categoryService.getCategoriesByTypeForCurrentUser("pemasukan");
        List<Category> pengeluaranCategories = categoryService.getCategoriesByTypeForCurrentUser("pengeluaran");

        if (pemasukanCategories.isEmpty()) {
            System.out.println("Membuat kategori pemasukan default 'Gaji' untuk testing...");
            Category gaji = new Category("Gaji (Test)", "pemasukan");
            gaji.setIsDefault(true); // Atau false jika ingin custom
            if(gaji.isIsDefault()) gaji.setUserId(null); else gaji.setUserId(testUserId);
            categoryService.saveNewCategory(gaji);
            pemasukanCategories = categoryService.getCategoriesByTypeForCurrentUser("pemasukan");
        }
        if (pengeluaranCategories.isEmpty()) {
            System.out.println("Membuat kategori pengeluaran default 'Makanan (Test)' untuk testing...");
            Category makanan = new Category("Makanan (Test)", "pengeluaran");
            makanan.setIsDefault(false); // Custom untuk user ini
            makanan.setUserId(testUserId);
            categoryService.saveNewCategory(makanan);
            pengeluaranCategories = categoryService.getCategoriesByTypeForCurrentUser("pengeluaran");
        }

        if (pemasukanCategories.isEmpty() || pengeluaranCategories.isEmpty()) {
            System.err.println("Kategori pemasukan atau pengeluaran tidak ditemukan/dibuat. Testing dibatalkan.");
            SessionManager.clearSession();
            return;
        }

        int gajiCategoryId = pemasukanCategories.get(0).getCategoryId();
        int makananCategoryId = pengeluaranCategories.get(0).getCategoryId();

        System.out.println("Menggunakan User ID: " + testUserId + " (Username: " + testUser.getUsername() + ")");
        System.out.println("Menggunakan Kategori Pemasukan ID: " + gajiCategoryId + " (Nama: " + pemasukanCategories.get(0).getName() + ")");
        System.out.println("Menggunakan Kategori Pengeluaran ID: " + makananCategoryId + " (Nama: " + pengeluaranCategories.get(0).getName() + ")");


        // 1. Simpan Transaksi Baru
        System.out.println("\n1. Menyimpan transaksi baru...");
        Transaction tx1 = new Transaction(testUserId, gajiCategoryId, 5000000,
                LocalDate.now().minusDays(5), "Gaji Bulan Ini", null, "pemasukan");
        Transaction tx2 = new Transaction(testUserId, makananCategoryId, 75000,
                LocalDate.now().minusDays(3), "Makan siang kantor", "/path/to/nota1.jpg", "pengeluaran");
        Transaction tx3 = new Transaction(testUserId, makananCategoryId, 120000,
                LocalDate.now().minusDays(1), "Belanja mingguan", null, "pengeluaran");

        boolean tx1Saved = transactionDao.saveTransaction(tx1);
        System.out.println("tx1 (Gaji) disimpan: " + tx1Saved + (tx1Saved ? " (ID: " + tx1.getTransactionId() + ")" : ""));
        boolean tx2Saved = transactionDao.saveTransaction(tx2);
        System.out.println("tx2 (Makan siang) disimpan: " + tx2Saved + (tx2Saved ? " (ID: " + tx2.getTransactionId() + ")" : ""));
        boolean tx3Saved = transactionDao.saveTransaction(tx3);
        System.out.println("tx3 (Belanja) disimpan: " + tx3Saved + (tx3Saved ? " (ID: " + tx3.getTransactionId() + ")" : ""));


        // 2. Tampilkan Semua Transaksi User (tanpa filter tambahan selain user ID)
        System.out.println("\n2. Semua transaksi untuk user ID " + testUserId + ":");
        List<Transaction> allTx = transactionDao.getAllTransactionsFiltered(testUserId, null, null, null, null);
        allTx.forEach(tx -> System.out.println("- " + tx.getDescription() + " | " + tx.getAmount() + " | " + tx.getTransactionDate()));

        // 3. Tampilkan Transaksi dengan Filter Tanggal
        System.out.println("\n3. Transaksi 2 hari terakhir untuk user ID " + testUserId + ":");
        List<Transaction> recentTx = transactionDao.getAllTransactionsFiltered(testUserId, LocalDate.now().minusDays(2), LocalDate.now(), null, null);
        recentTx.forEach(tx -> System.out.println("- " + tx.getDescription() + " | " + tx.getAmount() + " | " + tx.getTransactionDate()));

        // 4. Tampilkan Transaksi dengan Filter Kategori
        System.out.println("\n4. Transaksi kategori 'Makanan' untuk user ID " + testUserId + ":");
        List<Transaction> foodTx = transactionDao.getAllTransactionsFiltered(testUserId, null, null, makananCategoryId, null);
        foodTx.forEach(tx -> System.out.println("- " + tx.getDescription() + " | " + tx.getAmount() + " | " + tx.getTransactionDate()));

        // 5. Tampilkan Transaksi dengan Filter Keyword
        System.out.println("\n5. Transaksi dengan keyword 'siang' untuk user ID " + testUserId + ":");
        List<Transaction> keywordTx = transactionDao.getAllTransactionsFiltered(testUserId, null, null, null, "siang");
        keywordTx.forEach(tx -> System.out.println("- " + tx.getDescription() + " | " + tx.getAmount() + " | " + tx.getTransactionDate()));

        // 6. Update Transaksi
        if (!allTx.isEmpty() && tx2Saved) { // Gunakan transaksi yang pasti sudah tersimpan (tx2)
            System.out.println("\n6. Mengupdate transaksi ID " + tx2.getTransactionId() + " (Makan siang)...");
            Transaction txToUpdate = transactionDao.findTransactionById(tx2.getTransactionId()).orElse(null);
            if (txToUpdate != null) {
                String originalDesc = txToUpdate.getDescription();
                txToUpdate.setDescription("Makan siang TRAKTIRAN");
                txToUpdate.setAmount(85000);
                if (transactionDao.updateTransaction(txToUpdate)) {
                    System.out.println("Transaksi berhasil diupdate. Deskripsi baru: " + txToUpdate.getDescription());
                } else {
                    System.out.println("Gagal mengupdate transaksi.");
                    txToUpdate.setDescription(originalDesc); // Kembalikan jika gagal (meskipun tidak disimpan kembali)
                }
            } else {
                System.out.println("Transaksi dengan ID " + tx2.getTransactionId() + " tidak ditemukan untuk diupdate.");
            }
        }

        // 7. Hapus Transaksi
        if (tx3Saved) { // Gunakan transaksi yang pasti sudah tersimpan (tx3)
            System.out.println("\n7. Menghapus transaksi ID " + tx3.getTransactionId() + " (Belanja)...");
            if (transactionDao.deleteTransaction(tx3.getTransactionId(), testUserId)) {
                System.out.println("Transaksi berhasil dihapus.");
            } else {
                System.out.println("Gagal menghapus transaksi.");
            }
        }

        // Tampilkan semua transaksi lagi setelah update dan delete
        System.out.println("\nSemua transaksi untuk user ID " + testUserId + " (setelah update & delete):");
        allTx = transactionDao.getAllTransactionsFiltered(testUserId, null, null, null, null);
        allTx.forEach(tx -> System.out.println("- " + tx.getDescription() + " | " + tx.getAmount() + " | " + tx.getTransactionDate()));

        SessionManager.clearSession(); // Bersihkan sesi setelah testing
        System.out.println("\n--- Testing Transaction DAO Selesai ---");
    }
}