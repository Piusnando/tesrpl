package org.neracaku.neracaku.controllers; // PASTIKAN PACKAGE SESUAI

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage; // Untuk navigasi
import org.neracaku.neracaku.models.User; // Import User
import org.neracaku.neracaku.services.AuthService; // Import AuthService
import org.neracaku.neracaku.utils.NavigationUtil; // Import NavigationUtil
import org.neracaku.neracaku.utils.SessionManager; // Import SessionManager

public class PinController {

    @FXML
    private Label welcomeLabel; // Untuk menampilkan nama pengguna

    @FXML
    private PasswordField pinField;

    @FXML
    private Label pinErrorLabel;

    @FXML
    private Button verifyPinButton;

    @FXML
    private Button logoutButton; // Tombol kembali ke login

    private AuthService authService;

    @FXML
    public void initialize() {
        authService = new AuthService();
        hideError(); // Sembunyikan error label saat awal

        // Tampilkan nama pengguna jika ada sesi
        if (SessionManager.isLoggedIn()) {
            User currentUser = SessionManager.getCurrentUser();
            welcomeLabel.setText("Verifikasi PIN, " + currentUser.getUsername());
        } else {
            // Seharusnya tidak terjadi jika alur benar, tapi sebagai fallback
            welcomeLabel.setText("Verifikasi PIN");
            // Mungkin langsung kembali ke login jika tidak ada sesi
            // handleLogoutButtonAction(null); // Perlu ActionEvent jika dipanggil dari sini
        }
        System.out.println("PinController initialized.");
    }

    @FXML
    private void handleVerifyPinButtonAction(ActionEvent event) {
        String pin = pinField.getText();

        if (pin.isEmpty()) {
            showError("PIN tidak boleh kosong.");
            pinField.requestFocus();
            return;
        }
        // Anda bisa menambahkan validasi lain, misal panjang PIN harus 4 digit
        if (pin.length() != 4 && pin.length() != 6) { // Contoh, izinkan 4 atau 6 digit
            showError("PIN harus terdiri dari 4 atau 6 digit.");
            pinField.requestFocus();
            return;
        }


        hideError();

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("Sesi pengguna tidak ditemukan. Silakan login kembali.");
            // Mungkin nonaktifkan tombol atau langsung kembali ke login
            verifyPinButton.setDisable(true);
            logoutButton.requestFocus();
            return;
        }

        System.out.println("Mencoba verifikasi PIN untuk: " + currentUser.getUsername());

        if (authService.verifyPin(currentUser, pin)) {
            System.out.println("Verifikasi PIN berhasil untuk pengguna: " + currentUser.getUsername());
            // TODO: Navigasi ke halaman utama aplikasi (misalnya MainView.fxml atau DashboardView.fxml)
            showSuccess("PIN Benar! Mengarahkan ke aplikasi...");
            verifyPinButton.setDisable(true);
            logoutButton.setDisable(true); // Nonaktifkan juga tombol logout

            // Contoh navigasi ke MainView (buat file FXML dan Controller-nya nanti)
            Stage currentStage = NavigationUtil.getStageFromEvent(event);
            if (currentStage != null) {
                NavigationUtil.navigateTo("/org/neracaku/neracaku/fxml/MainView.fxml", "Neracaku - Dashboard", currentStage);
            }

        } else {
            System.out.println("Verifikasi PIN gagal untuk pengguna: " + currentUser.getUsername());
            showError("PIN salah. Silakan coba lagi.");
            pinField.clear();
            pinField.requestFocus();
            // TODO: Tambahkan logika batasan percobaan salah PIN
        }
    }

    @FXML
    private void handleLogoutButtonAction(ActionEvent event) {
        System.out.println("Tombol Logout (Kembali ke Login) diklik dari halaman PIN.");
        SessionManager.clearSession(); // Hapus sesi pengguna

        Stage currentStage = NavigationUtil.getStageFromEvent(event);
        if (currentStage != null) {
            NavigationUtil.navigateTo("/org/neracaku/neracaku/fxml/LoginView.fxml", "Neracaku - Login", currentStage);
        }
    }

    private void showError(String message) {
        pinErrorLabel.setText(message);
        pinErrorLabel.setStyle("-fx-text-fill: #ff5555;"); // Warna dari CSS (destructive)
        pinErrorLabel.setVisible(true);
        pinErrorLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        pinErrorLabel.setText(message);
        pinErrorLabel.setStyle("-fx-text-fill: #22c55e;"); // Warna hijau sukses
        pinErrorLabel.setVisible(true);
        pinErrorLabel.setManaged(true);
    }

    private void hideError() {
        pinErrorLabel.setText("");
        pinErrorLabel.setVisible(false);
        pinErrorLabel.setManaged(false);
    }
}