package org.neracaku.neracaku.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage; // Import Stage
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.services.AuthService;
import org.neracaku.neracaku.utils.NavigationUtil; // Import NavigationUtil
import org.neracaku.neracaku.utils.SessionManager;

import java.util.Optional;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Hyperlink registerLink;

    private AuthService authService;

    @FXML
    public void initialize() {
        authService = new AuthService();
        hideError();
        System.out.println("LoginController initialized and AuthService instantiated.");
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) { // Tambahkan parameter event
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            showError("Username tidak boleh kosong.");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Password tidak boleh kosong.");
            passwordField.requestFocus();
            return;
        }

        hideError();
        System.out.println("Mencoba proses login untuk: " + username);

        Optional<User> loginResult = authService.login(username, password);

        if (loginResult.isPresent()) {
            User loggedInUser = loginResult.get();
            SessionManager.setCurrentUser(loggedInUser);

            System.out.println("Login berhasil! Pengguna: " + loggedInUser.getUsername() + ", Role: " + loggedInUser.getRole());

            // Navigasi ke halaman PIN
            Stage currentStage = NavigationUtil.getStageFromEvent(event); // Dapatkan stage saat ini
            if (currentStage != null) {
                NavigationUtil.navigateTo("/org/neracaku/neracaku/fxml/PinView.fxml", "Verifikasi PIN", currentStage);
            } else {
                showError("Tidak dapat melakukan navigasi."); // Seharusnya tidak terjadi
            }

        } else {
            System.out.println("Login gagal. Username atau password mungkin salah.");
            showError("Username atau password salah. Silakan coba lagi.");
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    @FXML
    private void handleRegisterLinkAction(ActionEvent event) {
        System.out.println("Link Registrasi diklik.");
        Stage currentStage = NavigationUtil.getStageFromEvent(event);
        if (currentStage != null) {
            // Kita akan membuat RegisterView.fxml
            NavigationUtil.navigateTo("/org/neracaku/neracaku/fxml/RegisterView.fxml", "Neracaku - Registrasi Pengguna", currentStage);
        } else {
            showError("Tidak dapat membuka halaman registrasi.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #ef4444;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    // showSuccess tidak kita gunakan lagi karena langsung navigasi
    // private void showSuccess(String message) { ... }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}