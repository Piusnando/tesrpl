// Di RegisterController.java
package org.neracaku.neracaku.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert; // IMPORT Alert
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.neracaku.neracaku.services.UserService; // Pastikan ini diimport
import org.neracaku.neracaku.utils.NavigationUtil;

public class RegisterController {

    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmPasswordField;
    @FXML private PasswordField regPinField;
    @FXML private PasswordField regConfirmPinField;
    @FXML private Label regErrorLabel;
    // @FXML private Button registerButton; // Tidak perlu diakses langsung jika hanya trigger
    // @FXML private Button backToLoginButton; // Tidak perlu diakses langsung

    private UserService userService; // Deklarasikan UserService

    @FXML
    public void initialize() {
        userService = new UserService(); // Inisialisasi UserService
        System.out.println("RegisterController initialized.");
        hideError();
    }

    @FXML
    private void handleRegisterButtonAction(ActionEvent event) {
        String username = regUsernameField.getText().trim();
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();
        String pin = regPinField.getText();
        String confirmPin = regConfirmPinField.getText();

        // 1. Validasi input
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || pin.isEmpty() || confirmPin.isEmpty()) {
            showError("Semua field harus diisi.");
            return;
        }
        if (username.length() < 5) {
            showError("Username minimal 5 karakter.");
            return;
        }
        if (password.length() < 6) {
            showError("Password minimal 6 karakter.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Password dan konfirmasi password tidak cocok.");
            regPasswordField.clear();
            regConfirmPasswordField.clear();
            regPasswordField.requestFocus();
            return;
        }
        if (!pin.matches("\\d{4}|\\d{6}")) { // PIN harus 4 atau 6 digit angka
            showError("PIN harus terdiri dari 4 atau 6 digit angka.");
            regPinField.clear();
            regConfirmPinField.clear();
            regPinField.requestFocus();
            return;
        }
        if (!pin.equals(confirmPin)) {
            showError("PIN dan konfirmasi PIN tidak cocok.");
            regPinField.clear();
            regConfirmPinField.clear();
            regPinField.requestFocus();
            return;
        }

        hideError(); // Bersihkan error sebelumnya jika validasi dasar lolos

        // 2. Cek username sudah ada atau belum (panggil service)
        if (userService.isUsernameTaken(username)) {
            showError("Username '" + username + "' sudah digunakan. Silakan pilih username lain.");
            regUsernameField.requestFocus();
            return;
        }

        // 3. Panggil service untuk registrasi user
        System.out.println("Mencoba registrasi pengguna: " + username);
        boolean registrationSuccessful = userService.registerUser(username, password, pin);

        if (registrationSuccessful) {
            showAlert(Alert.AlertType.INFORMATION, "Registrasi Berhasil",
                    "Akun Anda untuk '" + username + "' telah berhasil dibuat. Silakan login.");
            handleBackToLoginAction(event); // Kembali ke login
        } else {
            // Pesan error lebih spesifik mungkin sudah dicetak oleh service,
            // tapi tampilkan pesan umum di UI.
            showError("Registrasi gagal. Terjadi kesalahan internal atau username sudah digunakan.");
        }
    }

    @FXML
    private void handleBackToLoginAction(ActionEvent event) {
        // ... (kode handleBackToLoginAction Anda sudah baik) ...
        System.out.println("Kembali ke Login dari halaman Registrasi.");
        Stage currentStage = NavigationUtil.getStageFromEvent(event);
        if (currentStage != null) {
            NavigationUtil.navigateTo("/org/neracaku/neracaku/fxml/LoginView.fxml", "Neracaku - Login", currentStage);
        }
    }

    private void showError(String message) {
        // ... (kode showError Anda sudah baik) ...
        regErrorLabel.setText(message);
        regErrorLabel.setVisible(true);
        regErrorLabel.setManaged(true);
    }

    private void hideError() {
        // ... (kode hideError Anda sudah baik) ...
        regErrorLabel.setText("");
        regErrorLabel.setVisible(false);
        regErrorLabel.setManaged(false);
    }

    // Helper untuk menampilkan Alert
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}