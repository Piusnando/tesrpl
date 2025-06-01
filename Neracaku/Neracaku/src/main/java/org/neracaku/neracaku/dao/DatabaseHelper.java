package org.neracaku.neracaku.dao; // PASTIKAN PACKAGE INI SESUAI

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {

    // Nama file database
    private static final String DB_FILE_NAME = "neracaku_data.db";

    // URL koneksi JDBC untuk SQLite.
    // System.getProperty("user.dir") mengembalikan direktori kerja saat ini,
    // yang dalam konteks pengembangan IntelliJ biasanya adalah root folder proyek.
    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.dir") + "/" + DB_FILE_NAME;

    public static Connection getConnection() throws SQLException {
        // Tidak perlu membuat folder khusus karena file akan berada di root proyek.
        // Driver SQLite biasanya ter-load otomatis jika JAR ada di classpath.
        // Class.forName("org.sqlite.JDBC"); // Baris ini seringkali tidak diperlukan lagi.
        return DriverManager.getConnection(DB_URL);
    }

    public static void createTablesIfNotExists() {
        String usersTableSql = "CREATE TABLE IF NOT EXISTS users ("
                + "user_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password_hash TEXT NOT NULL,"
                + "pin_hash TEXT NOT NULL,"
                + "role TEXT NOT NULL CHECK(role IN ('user', 'admin')),"
                + "created_at TEXT DEFAULT (strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime'))"
                + ");";

        String categoriesTableSql = "CREATE TABLE IF NOT EXISTS categories ("
                + "category_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER,"
                + "name TEXT NOT NULL,"
                + "type TEXT NOT NULL CHECK(type IN ('pemasukan', 'pengeluaran')),"
                + "is_default INTEGER DEFAULT 0,"
                + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,"
                + "UNIQUE (user_id, name, type)"
                + ");";

        String transactionsTableSql = "CREATE TABLE IF NOT EXISTS transactions ("
                + "transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL,"
                + "category_id INTEGER NOT NULL,"
                + "amount REAL NOT NULL,"
                + "transaction_date TEXT NOT NULL," // Format 'YYYY-MM-DD'
                + "description TEXT,"
                + "image_path TEXT,"
                + "type TEXT NOT NULL CHECK(type IN ('pemasukan', 'pengeluaran')),"
                + "created_at TEXT DEFAULT (strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')),"
                + "updated_at TEXT DEFAULT (strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')),"
                + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,"
                + "FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT"
                + ");";

        String transactionsUpdateTriggerSql = "CREATE TRIGGER IF NOT EXISTS update_transactions_updated_at "
                + "AFTER UPDATE ON transactions FOR EACH ROW BEGIN "
                + "UPDATE transactions SET updated_at = (strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')) WHERE transaction_id = OLD.transaction_id; "
                + "END;";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTableSql);
            System.out.println("Tabel 'users' diperiksa/dibuat.");
            stmt.execute(categoriesTableSql);
            System.out.println("Tabel 'categories' diperiksa/dibuat.");
            stmt.execute(transactionsTableSql);
            System.out.println("Tabel 'transactions' diperiksa/dibuat.");
            stmt.execute(transactionsUpdateTriggerSql);
            System.out.println("Trigger 'update_transactions_updated_at' diperiksa/dibuat.");

        } catch (SQLException e) {
            System.err.println("Error saat membuat tabel atau trigger: " + e.getMessage());
            e.printStackTrace(); // Untuk detail error yang lebih lengkap
        }
    }

    // Method main ini HANYA untuk testing awal pembuatan DB dan tabel.
    // Setelah DB berhasil dibuat di lokasi baru, Anda bisa menghapus atau mengomentari method main ini.
    // Nantinya, createTablesIfNotExists() akan dipanggil dari MainApp.
    public static void main(String[] args) {
        System.out.println("Mencoba membuat database dan tabel di root proyek...");
        System.out.println("URL Database akan menjadi: " + DB_URL);
        createTablesIfNotExists();
        System.out.println("Proses selesai. Periksa root folder proyek Anda untuk file '" + DB_FILE_NAME + "'.");
    }
}