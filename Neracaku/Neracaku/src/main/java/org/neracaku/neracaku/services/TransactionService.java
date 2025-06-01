package org.neracaku.neracaku.services; // PASTIKAN PACKAGE SESUAI

import org.neracaku.neracaku.dao.CategoryDao; // Mungkin dibutuhkan untuk validasi kategori
import org.neracaku.neracaku.dao.TransactionDao;
import org.neracaku.neracaku.dao.UserDao;
import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.models.MonthlySummary;
import org.neracaku.neracaku.models.Transaction;
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.utils.SessionManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionService {

    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao; // Untuk mendapatkan info kategori (misal, tipe)

    public TransactionService() {
        this.transactionDao = new TransactionDao();
        this.categoryDao = new CategoryDao(); // Inisialisasi CategoryDao
    }

    /**
     * Menyimpan transaksi baru.
     *
     * @param transaction Objek Transaction yang akan disimpan.
     * @return true jika berhasil, false jika gagal atau validasi tidak terpenuhi.
     */
    public boolean saveNewTransaction(Transaction transaction) {
        if (transaction == null) {
            System.err.println("Objek transaksi tidak boleh null.");
            return false;
        }
        if (!SessionManager.isLoggedIn()) {
            System.err.println("Tidak ada pengguna yang login untuk menyimpan transaksi.");
            return false;
        }
        transaction.setUserId(SessionManager.getCurrentUserId());

        // Validasi dasar
        if (transaction.getCategoryId() <= 0) {
            System.err.println("Kategori harus dipilih untuk transaksi.");
            return false;
        }
        if (transaction.getAmount() <= 0) {
            System.err.println("Jumlah transaksi harus lebih besar dari nol.");
            return false;
        }
        if (transaction.getTransactionDate() == null) {
            System.err.println("Tanggal transaksi tidak boleh kosong.");
            return false;
        }
        if (transaction.getTransactionDate().isAfter(LocalDate.now())) {
            System.err.println("Tanggal transaksi tidak boleh di masa depan.");
            return false;
        }
        // Validasi deskripsi (misal, tidak terlalu panjang, meskipun DB mungkin handle)
        if (transaction.getDescription() != null && transaction.getDescription().length() > 255) { // Contoh batas
            System.err.println("Deskripsi transaksi terlalu panjang (maks 255 karakter).");
            return false;
        }

        // Dapatkan tipe dari kategori yang dipilih
        Optional<Category> categoryOpt = categoryDao.findCategoryById(transaction.getCategoryId());
        if (categoryOpt.isEmpty()) {
            System.err.println("Kategori dengan ID " + transaction.getCategoryId() + " tidak ditemukan.");
            return false;
        }
        transaction.setType(categoryOpt.get().getType()); // Set tipe transaksi berdasarkan kategori

        // Validasi ukuran file gambar (jika ada) - contoh sederhana
        if (transaction.getImagePath() != null && !transaction.getImagePath().isEmpty()) {
            // Di sini Anda bisa menambahkan logika untuk memeriksa ukuran file sebenarnya
            // Untuk sekarang, kita asumsikan path valid jika tidak kosong
            // Batasan 5MB akan lebih baik divalidasi saat file dipilih di UI controller
            System.out.println("Transaksi memiliki path gambar: " + transaction.getImagePath());
        }

        return transactionDao.saveTransaction(transaction);
    }

    /**
     * Mengupdate transaksi yang sudah ada.
     *
     * @param transaction Objek Transaction dengan data yang sudah diupdate.
     * @return true jika berhasil, false jika gagal atau validasi tidak terpenuhi.
     */
    public boolean updateExistingTransaction(Transaction transaction) {
        if (transaction == null || transaction.getTransactionId() <= 0) {
            System.err.println("Data transaksi untuk update tidak valid (ID hilang).");
            return false;
        }
        if (!SessionManager.isLoggedIn() || transaction.getUserId() != SessionManager.getCurrentUserId()) {
            System.err.println("Pengguna tidak berhak mengedit transaksi ini atau sesi tidak valid.");
            return false; // Pastikan user yang login adalah pemilik transaksi
        }

        // Validasi serupa dengan saveNewTransaction
        if (transaction.getCategoryId() <= 0) { /* ... */ return false; }
        if (transaction.getAmount() <= 0) { /* ... */ return false; }
        if (transaction.getTransactionDate() == null) { /* ... */ return false; }
        if (transaction.getTransactionDate().isAfter(LocalDate.now())) { /* ... */ return false; }
        if (transaction.getDescription() != null && transaction.getDescription().length() > 255) { /* ... */ return false; }


        Optional<Category> categoryOpt = categoryDao.findCategoryById(transaction.getCategoryId());
        if (categoryOpt.isEmpty()) { /* ... */ return false; }
        transaction.setType(categoryOpt.get().getType());

        return transactionDao.updateTransaction(transaction);
    }

    /**
     * Menghapus transaksi.
     *
     * @param transactionId ID transaksi yang akan dihapus.
     * @return true jika berhasil, false jika gagal.
     */
    public boolean deleteTransaction(int transactionId) {
        if (!SessionManager.isLoggedIn()) {
            System.err.println("Tidak ada pengguna yang login untuk menghapus transaksi.");
            return false;
        }
        // DAO akan memastikan hanya user pemilik yang bisa menghapus
        return transactionDao.deleteTransaction(transactionId, SessionManager.getCurrentUserId());
    }

    /**
     * Mendapatkan transaksi berdasarkan ID.
     * @param transactionId ID transaksi.
     * @return Optional<Transaction>
     */
    public Optional<Transaction> getTransactionById(int transactionId) {
        if (!SessionManager.isLoggedIn()) return Optional.empty();
        Optional<Transaction> txOpt = transactionDao.findTransactionById(transactionId);
        // Pastikan transaksi yang diambil adalah milik user yang sedang login
        if (txOpt.isPresent() && txOpt.get().getUserId() == SessionManager.getCurrentUserId()) {
            return txOpt;
        }
        return Optional.empty();
    }


    /**
     * Mendapatkan transaksi terfilter untuk pengguna yang sedang login.
     * Menerapkan batasan riwayat 12 bulan jika startDate tidak ditentukan.
     *
     * @param startDate Tanggal mulai (opsional).
     * @param endDate Tanggal akhir (opsional, default hari ini).
     * @param categoryId ID kategori untuk filter (opsional).
     * @param keyword Kata kunci untuk pencarian (opsional).
     * @return List<Transaction>
     */
    public List<Transaction> getFilteredTransactionsForCurrentUser(LocalDate startDate, LocalDate endDate,
                                                                   Integer categoryId, String keyword) {
        if (!SessionManager.isLoggedIn()) {
            return new ArrayList<>();
        }
        int userId = SessionManager.getCurrentUserId();

        // Atur default endDate ke hari ini jika null
        LocalDate effectiveEndDate = (endDate == null) ? LocalDate.now() : endDate;

        // Atur default startDate ke 12 bulan lalu dari effectiveEndDate jika startDate null
        LocalDate effectiveStartDate = startDate;
        if (effectiveStartDate == null) {
            effectiveStartDate = effectiveEndDate.minusMonths(12).withDayOfMonth(1); // Awal bulan, 12 bulan lalu
        }

        // Pastikan startDate tidak lebih awal dari batas 12 bulan jika keduanya diberikan
        // dan startDate lebih awal dari (effectiveEndDate - 12 bulan)
        LocalDate twelveMonthsAgo = effectiveEndDate.minusMonths(12).withDayOfMonth(1);
        if (startDate != null && startDate.isBefore(twelveMonthsAgo)) {
            System.out.println("Rentang tanggal melebihi 12 bulan, startDate disesuaikan.");
            effectiveStartDate = twelveMonthsAgo;
        }
        // Pastikan endDate tidak di masa depan
        if (effectiveEndDate.isAfter(LocalDate.now())) {
            effectiveEndDate = LocalDate.now();
        }
        // Pastikan startDate tidak setelah endDate
        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            effectiveStartDate = effectiveEndDate; // Atau return list kosong / error
        }


        return transactionDao.getAllTransactionsFiltered(userId, effectiveStartDate, effectiveEndDate, categoryId, keyword);
    }

    /**
     * Mendapatkan total pengeluaran per kategori untuk bulan tertentu.
     * @param yearMonth Tahun dan bulan yang diinginkan.
     * @return Map<String, Double> di mana key adalah nama kategori dan value adalah total pengeluaran.
     */
    public Map<String, Double> getMonthlyExpensesByCategory(YearMonth yearMonth) {
        if (!SessionManager.isLoggedIn() || yearMonth == null) {
            return new HashMap<>(); // Kembalikan map kosong jika tidak ada sesi atau parameter tidak valid
        }
        int userId = SessionManager.getCurrentUserId();
        List<Transaction> monthlyExpenses = transactionDao.getMonthlyExpenseTransactions(userId, yearMonth.getYear(), yearMonth.getMonthValue());

        // Kelompokkan berdasarkan categoryId, lalu ubah categoryId menjadi nama kategori
        Map<Integer, Double> expensesByCategoryId = monthlyExpenses.stream()
                .collect(Collectors.groupingBy(Transaction::getCategoryId,
                        Collectors.summingDouble(Transaction::getAmount)));

        Map<String, Double> expensesByCategoryName = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : expensesByCategoryId.entrySet()) {
            Optional<Category> categoryOpt = categoryDao.findCategoryById(entry.getKey());
            String categoryName = categoryOpt.isPresent() ? categoryOpt.get().getName() : "Tanpa Kategori";
            expensesByCategoryName.put(categoryName, entry.getValue());
        }
        return expensesByCategoryName;
    }

    /**
     * Menghitung total saldo untuk pengguna saat ini berdasarkan semua transaksi mereka (dalam 12 bulan terakhir).
     * Saldo = Total Pemasukan - Total Pengeluaran.
     * @return double saldo.
     */
    public double getCurrentBalance() {
        if (!SessionManager.isLoggedIn()) return 0.0;

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(12).withDayOfMonth(1); // 12 bulan terakhir

        List<Transaction> transactions = transactionDao.getAllTransactionsFiltered(SessionManager.getCurrentUserId(), startDate, endDate, null, null);

        double totalPemasukan = 0.0;
        double totalPengeluaran = 0.0;

        for (Transaction tx : transactions) {
            if ("pemasukan".equalsIgnoreCase(tx.getType())) {
                totalPemasukan += tx.getAmount();
            } else if ("pengeluaran".equalsIgnoreCase(tx.getType())) {
                totalPengeluaran += tx.getAmount();
            }
        }
        return totalPemasukan - totalPengeluaran;
    }

    /**
     * Menghitung total pemasukan untuk pengguna saat ini dalam rentang waktu tertentu.
     * @param startDate Tanggal mulai.
     * @param endDate Tanggal akhir.
     * @return double total pemasukan.
     */
    public double getTotalPemasukan(LocalDate startDate, LocalDate endDate) {
        if (!SessionManager.isLoggedIn()) return 0.0;
        List<Transaction> transactions = transactionDao.getAllTransactionsFiltered(SessionManager.getCurrentUserId(), startDate, endDate, null, null);
        return transactions.stream()
                .filter(tx -> "pemasukan".equalsIgnoreCase(tx.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Menghitung total pengeluaran untuk pengguna saat ini dalam rentang waktu tertentu.
     * @param startDate Tanggal mulai.
     * @param endDate Tanggal akhir.
     * @return double total pengeluaran.
     */
    public double getTotalPengeluaran(LocalDate startDate, LocalDate endDate) {
        if (!SessionManager.isLoggedIn()) return 0.0;
        List<Transaction> transactions = transactionDao.getAllTransactionsFiltered(SessionManager.getCurrentUserId(), startDate, endDate, null, null);
        return transactions.stream()
                .filter(tx -> "pengeluaran".equalsIgnoreCase(tx.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Menghitung total pemasukan untuk pengguna saat ini untuk bulan lalu.
     * @return double total pemasukan bulan lalu.
     */
    public double getTotalPemasukanBulanLalu() {
        if (!SessionManager.isLoggedIn()) return 0.0;
        LocalDate bulanLalu = LocalDate.now().minusMonths(1);
        LocalDate startDateBulanLalu = bulanLalu.withDayOfMonth(1);
        LocalDate endDateBulanLalu = bulanLalu.withDayOfMonth(bulanLalu.lengthOfMonth());
        return getTotalPemasukan(startDateBulanLalu, endDateBulanLalu); // Memanggil method yang sudah ada
    }

    /**
     * Menghitung total pengeluaran untuk pengguna saat ini untuk bulan lalu.
     * @return double total pengeluaran bulan lalu.
     */
    public double getTotalPengeluaranBulanLalu() {
        if (!SessionManager.isLoggedIn()) return 0.0;
        LocalDate bulanLalu = LocalDate.now().minusMonths(1);
        LocalDate startDateBulanLalu = bulanLalu.withDayOfMonth(1);
        LocalDate endDateBulanLalu = bulanLalu.withDayOfMonth(bulanLalu.lengthOfMonth());
        return getTotalPengeluaran(startDateBulanLalu, endDateBulanLalu); // Memanggil method yang sudah ada
    }

    /**
     * Mendapatkan ringkasan pemasukan dan pengeluaran bulanan untuk beberapa bulan terakhir.
     * @param numberOfMonths Jumlah bulan terakhir yang akan diambil (termasuk bulan ini).
     * @return List dari MonthlySummary.
     */
    public List<MonthlySummary> getRecentMonthlySummaries(int numberOfMonths) {
        if (!SessionManager.isLoggedIn() || numberOfMonths <= 0) {
            return new ArrayList<>();
        }
        int userId = SessionManager.getCurrentUserId();
        return transactionDao.getMonthlySummaries(userId, numberOfMonths);

    }


    // --- Main method untuk testing TransactionService (opsional) ---
    public static void main(String[] args) {
        TransactionService transactionService = new TransactionService();
        // Sediakan dummy user dan kategori untuk testing
        UserDao userDao = new UserDao(); // Untuk setup
        AuthService authService = new AuthService(); // Untuk setup
        CategoryService categoryService = new CategoryService(); // Untuk setup kategori

        // 1. Pastikan admin user ada dan login
        authService.initializeAdminUser();
        Optional<User> adminOpt = userDao.findUserByUsername("admin");
        if (adminOpt.isEmpty()) {
            System.err.println("Gagal menginisialisasi/menemukan admin user untuk testing.");
            return;
        }
        SessionManager.setCurrentUser(adminOpt.get());
        System.out.println("Testing sebagai user: " + SessionManager.getCurrentUser().getUsername());

        // 2. Pastikan ada kategori
        if (categoryService.getCategoriesByTypeForCurrentUser("pemasukan").isEmpty()) {
            categoryService.saveNewCategory(new Category(0,"Gaji Service", "pemasukan", true, null));
        }
        if (categoryService.getCategoriesByTypeForCurrentUser("pengeluaran").isEmpty()) {
            categoryService.saveNewCategory(new Category(0,"Makanan Service", "pengeluaran", false, SessionManager.getCurrentUserId()));
        }
        int pemasukanCatId = categoryService.getCategoriesByTypeForCurrentUser("pemasukan").get(0).getCategoryId();
        int pengeluaranCatId = categoryService.getCategoriesByTypeForCurrentUser("pengeluaran").get(0).getCategoryId();


        // 3. Simpan transaksi baru
        System.out.println("\nMenyimpan transaksi baru via service...");
        Transaction txNew = new Transaction();
        // userId akan di-set oleh service
        txNew.setCategoryId(pemasukanCatId);
        txNew.setAmount(7000000);
        txNew.setTransactionDate(LocalDate.now().minusDays(2));
        txNew.setDescription("Pemasukan Gaji via Service");
        if (transactionService.saveNewTransaction(txNew)) {
            System.out.println("Transaksi baru berhasil disimpan: " + txNew.getDescription() + " (ID: " + txNew.getTransactionId() + ")");
        } else {
            System.out.println("Gagal menyimpan transaksi baru.");
        }

        Transaction txExpense = new Transaction();
        txExpense.setCategoryId(pengeluaranCatId);
        txExpense.setAmount(50000);
        txExpense.setTransactionDate(LocalDate.now().minusDays(1));
        txExpense.setDescription("Makan Siang Service");
        if (transactionService.saveNewTransaction(txExpense)) {
            System.out.println("Transaksi pengeluaran berhasil disimpan: " + txExpense.getDescription() + " (ID: " + txExpense.getTransactionId() + ")");
        } else {
            System.out.println("Gagal menyimpan transaksi pengeluaran.");
        }

        // 4. Dapatkan transaksi terfilter
        System.out.println("\nTransaksi dalam 5 hari terakhir:");
        List<Transaction> filtered = transactionService.getFilteredTransactionsForCurrentUser(
                LocalDate.now().minusDays(5), LocalDate.now(), null, null);
        filtered.forEach(tx -> System.out.println("- " + tx.getDescription() + " | " + tx.getAmount()));

        // 5. Dapatkan saldo saat ini
        System.out.println("\nSaldo Saat Ini: " + transactionService.getCurrentBalance());

        // 6. Dapatkan total pemasukan & pengeluaran bulan ini
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        System.out.println("Total Pemasukan Bulan Ini: " + transactionService.getTotalPemasukan(startOfMonth, endOfMonth));
        System.out.println("Total Pengeluaran Bulan Ini: " + transactionService.getTotalPengeluaran(startOfMonth, endOfMonth));


        // 7. Update transaksi
        if (!filtered.isEmpty()) {
            Transaction toUpdate = filtered.get(0); // Ambil transaksi pertama dari hasil filter
            System.out.println("\nMengupdate transaksi: " + toUpdate.getDescription());
            toUpdate.setDescription(toUpdate.getDescription() + " (UPDATED)");
            if(transactionService.updateExistingTransaction(toUpdate)){
                System.out.println("Berhasil diupdate.");
            } else {
                System.out.println("Gagal update.");
            }
        }

        // 8. Hapus transaksi
        if (txExpense.getTransactionId() > 0) { // Hapus transaksi pengeluaran yang baru dibuat
            System.out.println("\nMenghapus transaksi: " + txExpense.getDescription());
            if(transactionService.deleteTransaction(txExpense.getTransactionId())){
                System.out.println("Berhasil dihapus.");
            } else {
                System.out.println("Gagal hapus.");
            }
        }


        SessionManager.clearSession();
        System.out.println("\n--- Testing Transaction Service Selesai ---");
    }
}