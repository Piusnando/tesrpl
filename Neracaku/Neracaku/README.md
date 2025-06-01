# Neracaku - Aplikasi Pencatatan Keuangan Pribadi

Neracaku adalah aplikasi desktop yang dirancang untuk membantu pengguna mencatat dan mengelola keuangan pribadi mereka dengan mudah. Dengan antarmuka yang user-friendly, pengguna dapat melacak pemasukan dan pengeluaran, menganalisis kebiasaan belanja, dan mendapatkan gambaran kondisi keuangan secara real-time untuk pengambilan keputusan finansial yang lebih baik.

## Daftar Isi

1. [Fitur Utama](#fitur-utama)
2. [Teknologi yang Digunakan](#teknologi-yang-digunakan)
3. [Arsitektur Aplikasi](#arsitektur-aplikasi)
    - [Struktur Package](#struktur-package)
    - [Lapisan Aplikasi (Layers)](#lapisan-aplikasi-layers)
4. [Desain Database](#desain-database)
5. [Alur Kerja Aplikasi (Application Flow)](#alur-kerja-aplikasi-application-flow)
    - [Startup dan Inisialisasi](#startup-dan-inisialisasi)
    - [Registrasi Pengguna Baru](#registrasi-pengguna-baru)
    - [Login Pengguna](#login-pengguna)
    - [Verifikasi PIN](#verifikasi-pin)
    - [Navigasi Utama](#navigasi-utama)
    - [Manajemen Kategori](#manajemen-kategori)
    - [Manajemen Transaksi](#manajemen-transaksi)
    - [Dashboard](#dashboard)
    - [Laporan](#laporan)
    - [Manajemen User oleh Admin](#manajemen-user-oleh-admin)
    - [Logout](#logout)
6. [Logika Kunci dan Komponen Penting](#logika-kunci-dan-komponen-penting)
    - [Otentikasi dan Keamanan](#otentikasi-dan-keamanan)
    - [Manajemen Sesi](#manajemen-sesi)
    - [Validasi Data](#validasi-data)
    - [Navigasi UI](#navigasi-ui)
    - [Pengelolaan Data Keuangan](#pengelolaan-data-keuangan)
7. [Batasan Sistem](#batasan-sistem)
8. [Instalasi dan Penggunaan](#instalasi-dan-penggunaan)
9. [Kontribusi](#kontribusi)
10. [Lisensi](#lisensi)

---

## Fitur Utama

- **Pencatatan Transaksi:** Pengguna dapat menambahkan transaksi pemasukan dan pengeluaran dengan detail seperti tanggal, jumlah, kategori, deskripsi, dan lampiran gambar (nota/kwitansi).
- **Manajemen Kategori:** Pengguna dapat membuat, mengedit, dan menghapus kategori transaksi mereka sendiri (misalnya, Makanan, Transportasi, Gaji, Investasi). Kategori default sistem juga tersedia.
- **Daftar Transaksi Fleksibel:** Menampilkan riwayat transaksi dengan opsi filter berdasarkan rentang waktu, kategori, dan pencarian berdasarkan kata kunci.
- **Dashboard Ringkasan Keuangan:** Menampilkan gambaran kondisi keuangan secara real-time, termasuk:
    - Saldo saat ini (berdasarkan 12 bulan terakhir).
    - Total pemasukan dan pengeluaran bulan ini, dengan perbandingan terhadap bulan sebelumnya.
    - Grafik Pie Chart pengeluaran per kategori untuk bulan berjalan.
- **Laporan Transaksi:** Menghasilkan laporan tabular transaksi berdasarkan periode dan kategori yang dipilih, lengkap dengan ringkasan total.
- **Ekspor Laporan ke CSV:** Memungkinkan pengguna mengekspor data laporan transaksi ke format CSV untuk analisis lebih lanjut.
- **Keamanan Akun:**
    - **Login Pengguna:** Sistem login dengan username dan password.
    - **PIN Tambahan:** Lapisan keamanan kedua menggunakan PIN setelah login berhasil.
    - Password dan PIN di-hash menggunakan BCrypt.
- **Registrasi Pengguna:** Pengguna baru dapat mendaftar untuk membuat akun.
- **Manajemen User oleh Admin (Role Admin):**
    - Melihat daftar semua pengguna.
    - Menambah pengguna baru (dengan role user/admin).
    - Mengedit informasi pengguna (misalnya, mereset password/PIN, mengubah role).
    - Menghapus akun pengguna.
    - Mengelola kategori default sistem.
- **Antarmuka Pengguna Modern:** Menggunakan tema gelap yang terinspirasi dari ShadCN/UI untuk pengalaman pengguna yang nyaman.

---

## Teknologi yang Digunakan

- **Bahasa Pemrograman:** Java (OpenJDK 22)
- **Framework UI:** JavaFX (OpenJFX)
- **Build Tool & Dependency Management:** Apache Maven
- **Desain UI (Markup):** FXML
- **Styling UI:** CSS JavaFX
- **Database:** SQLite (Embedded, file-based)
- **JDBC Driver:** SQLite JDBC Driver (org.xerial:sqlite-jdbc)
- **Password Hashing:** jBCrypt
- **Ikon:** Ikonli (FontAwesome 5 Pack)
- **IDE:** IntelliJ IDEA

---

## Arsitektur Aplikasi

Aplikasi ini dirancang dengan pendekatan berlapis untuk memisahkan tanggung jawab dan meningkatkan keterbacaan serta kemudahan pemeliharaan kode.

### Struktur Package

```
org.neracaku.neracaku/
├── MainApp.java                # Kelas utama aplikasi JavaFX
├── controllers/                # Kontroler JavaFX untuk setiap FXML view
│   ├── LoginController.java
│   ├── PinController.java
│   ├── MainViewController.java
│   ├── DashboardController.java
│   ├── TransactionListController.java
│   ├── AddEditTransactionDialogController.java
│   ├── CategoryController.java
│   ├── AddEditCategoryDialogController.java
│   ├── ReportController.java
│   ├── AdminUserManagementController.java
│   └── AddEditUserByAdminDialogController.java
├── dao/                        # Data Access Objects (interaksi dengan database)
│   ├── DatabaseHelper.java     # Koneksi dan pembuatan tabel
│   ├── UserDao.java
│   ├── CategoryDao.java
│   └── TransactionDao.java
├── models/                     # Kelas-kelas model (representasi data)
│   ├── User.java
│   ├── Category.java
│   └── Transaction.java
├── services/                   # Lapisan layanan (logika bisnis)
│   ├── AuthService.java
│   ├── UserService.java
│   ├── CategoryService.java
│   ├── TransactionService.java
│   └── ReportService.java      # (Jika ada logika khusus laporan)
└── utils/                      # Kelas-kelas utilitas
    ├── NavigationUtil.java     # Navigasi antar FXML
    ├── PasswordUtil.java       # Hashing password & PIN
    ├── SessionManager.java     # Mengelola sesi pengguna yang login
    └── DateUtil.java           # (Opsional, untuk utilitas tanggal)
```

File FXML dan CSS ditempatkan di direktori `src/main/resources/org/neracaku/neracaku/` dengan sub-folder `fxml/` dan `css/`.

### Lapisan Aplikasi (Layers)

1. **Presentation Layer (View & Controller):**
    - **FXML (`*.fxml`):** Mendefinisikan struktur dan tata letak antarmuka pengguna.
    - **Controllers (`*.java` di `controllers/`):** Menangani input pengguna, memperbarui view, dan berinteraksi dengan Service Layer.

2. **Service Layer (`*.java` di `services/`):**
    - Berisi logika bisnis inti aplikasi.
    - Mengkoordinasikan interaksi antara Controller dan DAO.
    - Melakukan validasi data yang lebih kompleks.

3. **Data Access Layer (DAO) (`*.java` di `dao/`):**
    - Bertanggung jawab untuk semua operasi database (CRUD - Create, Read, Update, Delete).
    - Berinteraksi langsung dengan database SQLite menggunakan JDBC.
    - `DatabaseHelper.java` mengelola koneksi dan skema tabel.

4. **Model Layer (`*.java` di `models/`):**
    - Plain Old Java Objects (POJO) yang merepresentasikan entitas data aplikasi (User, Category, Transaction).

5. **Utility Layer (`*.java` di `utils/`):**
    - Kelas-kelas pembantu untuk fungsionalitas umum seperti hashing, navigasi, dan manajemen sesi.

---

## Desain Database

Database SQLite disimpan dalam satu file (`neracaku_data.db`) di root direktori aplikasi untuk portabilitas.

- **`users`**: Menyimpan informasi kredensial dan role pengguna.
    - `user_id` (PK, AI), `username` (UNIQUE), `password_hash`, `pin_hash`, `role` ('user' atau 'admin'), `created_at`.

- **`categories`**: Menyimpan kategori transaksi.
    - `category_id` (PK, AI), `user_id` (FK, bisa NULL untuk default), `name`, `type` ('pemasukan' atau 'pengeluaran'), `is_default` (boolean).
    - Constraint `UNIQUE` pada (`user_id`, `name`, `type`).

- **`transactions`**: Menyimpan detail setiap transaksi keuangan.
    - `transaction_id` (PK, AI), `user_id` (FK), `category_id` (FK), `amount` (REAL), `transaction_date` (TEXT, 'YYYY-MM-DD'), `description`, `image_path`, `type` ('pemasukan' atau 'pengeluaran'), `created_at`, `updated_at`.
    - Trigger `update_transactions_updated_at` otomatis memperbarui kolom `updated_at` saat baris diupdate.
    - Foreign key `ON DELETE CASCADE` dari `transactions` dan `categories` ke `users`.
    - Foreign key `ON DELETE RESTRICT` dari `transactions` ke `categories` untuk mencegah penghapusan kategori yang masih digunakan.

---

## Alur Kerja Aplikasi (Application Flow)

### Startup dan Inisialisasi

1. `MainApp.java` diluncurkan.
2. Method `init()`:
    - `DatabaseHelper.createTablesIfNotExists()`: Memastikan semua tabel database ada.
    - `AuthService.initializeAdminUser()`: Membuat akun admin default ("admin"/"admin123", PIN "1234") jika belum ada.
3. Method `start()`: Memuat dan menampilkan `LoginView.fxml`.

### Registrasi Pengguna Baru

1. Pengguna mengklik link/tombol "Registrasi" di `LoginView`.
2. `LoginController` menavigasi ke `RegisterView.fxml`.
3. Pengguna mengisi form registrasi (username, password, konfirmasi password, PIN, konfirmasi PIN).
4. `RegisterController` melakukan validasi input (field kosong, kecocokan password/PIN, format PIN, panjang minimal).
5. `RegisterController` memanggil `UserService.isUsernameTaken()` untuk memeriksa ketersediaan username.
6. Jika valid dan username tersedia, `RegisterController` memanggil `UserService.registerUser()`.
    - `UserService` menghash password dan PIN.
    - `UserService` memanggil `UserDao.saveUser()` untuk menyimpan pengguna baru dengan role "user".
7. Jika berhasil, pengguna diberi notifikasi dan diarahkan kembali ke `LoginView`.

### Login Pengguna

1. Pengguna memasukkan username dan password di `LoginView`.
2. `LoginController` memanggil `AuthService.login()`.
3. `AuthService` memanggil `UserDao.findUserByUsername()`.
4. Jika user ditemukan, `AuthService` membandingkan hash password menggunakan `PasswordUtil.checkPassword()`.
5. Jika berhasil:
    - `SessionManager.setCurrentUser()` menyimpan informasi pengguna yang login.
    - Navigasi ke `PinView.fxml`.
6. Jika gagal, pesan error ditampilkan di `LoginView`.

### Verifikasi PIN

1. Pengguna memasukkan PIN di `PinView`.
2. `PinController` memanggil `AuthService.verifyPin()` dengan `SessionManager.getCurrentUser()` dan PIN yang dimasukkan.
3. `AuthService` membandingkan hash PIN menggunakan `PasswordUtil.checkPin()`.
4. Jika berhasil:
    - Navigasi ke `MainView.fxml` (tampilan utama aplikasi).
5. Jika gagal, pesan error ditampilkan. Ada logika untuk kembali ke login setelah beberapa percobaan gagal (belum diimplementasikan sepenuhnya).

### Navigasi Utama

1. `MainView.fxml` bertindak sebagai "cangkang" dengan sidebar dan area konten (`contentAreaPane`).
2. `MainViewController` menangani klik pada tombol-tombol di sidebar.
3. Setiap tombol menu memanggil `MainViewController.loadContent(String fxmlPath)` yang memuat FXML yang sesuai (misalnya, `DashboardView.fxml`, `TransactionListView.fxml`) ke dalam `contentAreaPane`.
4. Tombol menu yang aktif diberi style class "active".

### Manajemen Kategori

1. Pengguna navigasi ke "Kategori" (`CategoryView.fxml`).
2. `CategoryController`:
    - Memanggil `CategoryService.getAllCategoriesForDisplay()` untuk memuat kategori default dan kategori milik pengguna saat ini.
    - Menampilkan kategori dalam `TableView`.
    - **Tambah Kategori:** Membuka `AddEditCategoryDialog.fxml` dalam mode tambah.
        - `AddEditCategoryDialogController` mengambil input nama dan tipe.
        - Memanggil `CategoryService.saveNewCategory()`.
        - `CategoryService` melakukan validasi dan memanggil `CategoryDao.saveCategory()`.
    - **Edit Kategori:** Membuka `AddEditCategoryDialog.fxml` dalam mode edit dengan data kategori terpilih.
        - Pengguna mengubah data.
        - `AddEditCategoryDialogController` memanggil `CategoryService.updateExistingCategory()`.
        - `CategoryService` melakukan validasi hak akses dan memanggil `CategoryDao.updateCategory()`.
    - **Hapus Kategori:** Menampilkan dialog konfirmasi.
        - Memanggil `CategoryService.deleteUserCategory()` (atau `deleteDefaultForAdmin()` jika admin).
        - `CategoryService` melakukan validasi hak akses dan memanggil method DAO yang sesuai.
    - `TableView` di-refresh setelah setiap operasi CRUD.

### Manajemen Transaksi

1. Pengguna navigasi ke "Transaksi" (`TransactionListView.fxml`).
2. `TransactionListController`:
    - Memanggil `TransactionService.getFilteredTransactionsForCurrentUser()` untuk memuat transaksi awal (misalnya, bulan ini) dan setiap kali filter diubah/diterapkan.
    - Menampilkan transaksi dalam `TableView`.
    - **Filter:** Pengguna bisa memfilter berdasarkan rentang tanggal, kategori, dan kata kunci deskripsi.
    - **Tambah Transaksi:** Membuka `AddEditTransactionDialog.fxml` dalam mode tambah.
        - `AddEditTransactionDialogController`: Pengguna mengisi form (jenis, tanggal, kategori, jumlah, deskripsi, gambar nota).
            - `ComboBox` kategori diisi dinamis berdasarkan jenis (pemasukan/pengeluaran) via `CategoryService`.
            - `FileChooser` digunakan untuk memilih gambar. Validasi ukuran file dilakukan.
            - Memanggil `TransactionService.saveNewTransaction()`.
            - `TransactionService` melakukan validasi, menentukan tipe transaksi dari kategori, dan memanggil `TransactionDao.saveTransaction()`.
    - **Edit Transaksi:** Membuka `AddEditTransactionDialog.fxml` dalam mode edit.
        - Logika mirip tambah, tetapi memanggil `TransactionService.updateExistingTransaction()`.
    - **Lihat Detail Transaksi:** Membuka `AddEditTransactionDialog.fxml` dalam mode *read-only*, menampilkan semua detail termasuk gambar nota.
    - **Hapus Transaksi:** Menampilkan dialog konfirmasi, lalu memanggil `TransactionService.deleteTransaction()`.
    - `TableView` di-refresh. Label summary (total pemasukan/pengeluaran/saldo periode) di-update.

### Dashboard

1. Pengguna navigasi ke "Dashboard" (`DashboardView.fxml`) atau ini adalah tampilan default setelah PIN.
2. `DashboardController`:
    - Memanggil `TransactionService` untuk mendapatkan:
        - `getCurrentBalance()`
        - `getTotalPemasukan(bulanIni)` dan `getTotalPemasukanBulanLalu()`
        - `getTotalPengeluaran(bulanIni)` dan `getTotalPengeluaranBulanLalu()`
        - `getMonthlyExpensesByCategory(bulanIni)`
    - Menampilkan data ini di kartu KPI, termasuk perbandingan persentase dengan bulan lalu.
    - Menampilkan Pie Chart pengeluaran per kategori untuk bulan ini.

### Laporan

1. Pengguna navigasi ke "Laporan" (`ReportView.fxml`).
2. `ReportController`:
    - Pengguna memilih filter periode (tanggal mulai, tanggal akhir) dan kategori (opsional).
    - Saat tombol "Tampilkan Laporan" diklik, memanggil `TransactionService.getFilteredTransactionsForCurrentUser()`.
    - Menampilkan hasil transaksi dalam `TableView`.
    - Menampilkan ringkasan total pemasukan, pengeluaran, dan saldo bersih untuk periode tersebut.
    - **Ekspor ke CSV:** Menggunakan `FileChooser`, data dari `TableView` ditulis ke file CSV.

### Manajemen User oleh Admin

1. Pengguna admin navigasi ke "Manajemen User" (`AdminUserManagementView.fxml`).
2. `AdminUserManagementController`:
    - Memanggil `UserService.getAllUsersForAdmin()` untuk menampilkan semua pengguna di `TableView`.
    - **Tambah User:** Membuka `AddEditUserByAdminDialog.fxml` dalam mode tambah.
        - `AddEditUserByAdminDialogController`: Admin mengisi username, password awal, PIN awal, dan role.
        - Memanggil `UserService.createUserByAdmin()`.
    - **Edit User:** Membuka `AddEditUserByAdminDialog.fxml` dalam mode edit.
        - Admin bisa mengubah username (dengan validasi keunikan), mereset password/PIN (dengan input baru), dan mengubah role (dengan batasan untuk akun admin utama).
        - Memanggil `UserService.updateUserByAdmin()`.
    - **Hapus User:** Menampilkan dialog konfirmasi.
        - Memanggil `UserService.deleteUserByAdmin()`. Admin tidak bisa menghapus dirinya sendiri.
    - `TableView` di-refresh.

### Logout

1. Pengguna mengklik tombol "Logout" di sidebar `MainView`.
2. `MainViewController.handleLogoutAction()`:
    - `SessionManager.clearSession()` menghapus informasi pengguna yang login.
    - Navigasi kembali ke `LoginView.fxml`.

---

## Logika Kunci dan Komponen Penting

### Otentikasi dan Keamanan

Password dan PIN tidak pernah disimpan sebagai plain text. `PasswordUtil` menggunakan BCrypt untuk hashing dengan salt yang di-generate secara otomatis. Setiap proses login dan verifikasi PIN melibatkan perbandingan hash.

### Manajemen Sesi

`SessionManager` menggunakan variabel statis untuk menyimpan objek `User` yang sedang login, memungkinkan akses global ke informasi pengguna saat ini (ID, username, role).

### Validasi Data

Validasi dasar (field tidak kosong, format, dll.) dilakukan di Controller. Validasi bisnis yang lebih kompleks (misalnya, username sudah ada, hak akses) dilakukan di Service Layer. Constraint database (UNIQUE, NOT NULL, FOREIGN KEY) memberikan lapisan validasi terakhir.

### Navigasi UI

`NavigationUtil.navigateTo()` digunakan untuk perpindahan antar FXML utama (mengganti scene pada stage yang sama). `MainViewController.loadContent()` digunakan untuk memuat FXML fitur ke dalam area konten `MainView` secara dinamis. Dialog modal (`Stage` baru dengan `initModality(Modality.WINDOW_MODAL)` dan `initOwner()`) digunakan untuk form tambah/edit.

### Pengelolaan Data Keuangan

`TransactionService` adalah pusat untuk logika perhitungan saldo, total pemasukan/pengeluaran, dan agregasi data untuk dashboard dan laporan. DAO menangani interaksi SQL murni.

---

## Batasan Sistem

1. Sistem hanya mendukung mata uang Rupiah (Rp) sebagai mata uang utama.
2. Aplikasi hanya dapat diakses melalui perangkat desktop (membutuhkan Java Runtime Environment).
3. Riwayat transaksi yang dapat ditampilkan dan dipertimbangkan untuk kalkulasi ringkasan (seperti saldo di dashboard) dibatasi maksimal 12 bulan ke belakang.
4. Ukuran file dokumen pendukung (nota/kwitansi gambar) dibatasi maksimal 5 MB per file.
5. Manajemen file gambar nota saat ini mungkin menyimpan path absolut, idealnya disempurnakan untuk portabilitas dengan menyalin file ke direktori data aplikasi.

---

## Instalasi dan Penggunaan

### Prasyarat

- Java Runtime Environment (JRE) versi 17 atau lebih tinggi (sesuaikan dengan versi JDK Anda).

### Cara Menjalankan

1. **Jika dari JAR:** `java -jar Neracaku.jar`
2. **Jika dari source:** Buka proyek di IntelliJ IDEA, pastikan dependensi Maven ter-resolve, lalu jalankan kelas `org.neracaku.neracaku.MainApp`.

### Penggunaan Awal

Saat pertama kali dijalankan, akun admin default akan dibuat:
- Username: `admin`
- Password: `admin123`
- PIN: `1234`

Pengguna dapat mendaftar untuk akun baru melalui link di halaman login.

---

## Kontribusi

Kontribusi terhadap proyek ini sangat diterima! Silakan buat pull request atau laporkan issue yang Anda temukan.

---

## Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE).