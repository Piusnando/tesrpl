package org.neracaku.neracaku.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.services.UserService;
import org.neracaku.neracaku.utils.PasswordUtil;
import org.neracaku.neracaku.utils.SessionManager;

import java.util.Optional;

public class AddEditUserByAdminDialogController {

    @FXML
    private Label dialogTitleUserLabel;
    @FXML
    private TextField usernameAdminField;
    @FXML
    private PasswordField passwordAdminField;
    @FXML
    private PasswordField pinAdminField;
    @FXML
    private ChoiceBox<String> roleAdminChoiceBox;
    @FXML
    private Label errorUserDialogLabel;
    @FXML
    private Button saveUserAdminButton;

    private UserService userService;
    private User userToEdit;
    private boolean isEditMode = false;

    public interface UserDialogHandler {
        void onUserSaved();
    }
    private UserDialogHandler dialogHandler;

    public void setDialogHandler(UserDialogHandler handler) {
        this.dialogHandler = handler;
    }

    @FXML
    public void initialize() {
        userService = new UserService();
        roleAdminChoiceBox.setItems(FXCollections.observableArrayList("user", "admin"));
        hideError();
        System.out.println("AddEditUserByAdminDialogController initialized.");
    }

    public void setNewUserMode() {
        this.isEditMode = false;
        this.userToEdit = null;
        dialogTitleUserLabel.setText("Tambah Pengguna Baru (Admin)");
        saveUserAdminButton.setText("Simpan");
        usernameAdminField.clear();
        passwordAdminField.clear();
        passwordAdminField.setPromptText("Min. 6 karakter (Wajib diisi)"); // Prompt berbeda untuk tambah
        pinAdminField.clear();
        pinAdminField.setPromptText("---- atau ------ (Wajib diisi)"); // Prompt berbeda untuk tambah
        roleAdminChoiceBox.setValue("user"); // Default role
        usernameAdminField.setEditable(true); // Username bisa diedit saat tambah
    }

    public void setUserToEdit(User user) {
        this.userToEdit = user;
        this.isEditMode = true;
        dialogTitleUserLabel.setText("Edit Pengguna: " + user.getUsername());
        saveUserAdminButton.setText("Update");

        usernameAdminField.setText(user.getUsername());
        passwordAdminField.clear();
        passwordAdminField.setPromptText("Kosongkan jika tidak diubah");
        pinAdminField.clear();
        pinAdminField.setPromptText("Kosongkan jika tidak diubah");
        roleAdminChoiceBox.setValue(user.getRole());

        // Admin tidak boleh mengubah username admin utama atau role admin utama menjadi user
        // Atau, username tidak bisa diubah sama sekali saat edit untuk menghindari kompleksitas
        if (user.getUsername().equals("admin")) { // Asumsi "admin" adalah super admin
            usernameAdminField.setEditable(false);
            if (user.getRole().equals("admin")) {
                roleAdminChoiceBox.setDisable(true); // Tidak bisa ubah role super admin
            }
        } else {
            usernameAdminField.setEditable(true); // Atau false jika username tidak boleh diubah
            roleAdminChoiceBox.setDisable(false);
        }
    }

    @FXML
    private void handleSaveUserAdminAction(ActionEvent event) {
        String username = usernameAdminField.getText().trim();
        String password = passwordAdminField.getText(); // Bisa kosong saat edit
        String pin = pinAdminField.getText();         // Bisa kosong saat edit
        String role = roleAdminChoiceBox.getValue();

        // Validasi dasar
        if (username.isEmpty()) {
            showError("Username tidak boleh kosong."); return;
        }
        if (role == null) {
            showError("Role harus dipilih."); return;
        }

        // Validasi untuk mode tambah baru (password & PIN wajib)
        if (!isEditMode) {
            if (password.isEmpty()) { showError("Password wajib diisi untuk pengguna baru."); return; }
            if (pin.isEmpty()) { showError("PIN wajib diisi untuk pengguna baru."); return; }
        }

        // Validasi panjang jika diisi
        if (username.length() < 5) { showError("Username minimal 5 karakter."); return; }
        if (!password.isEmpty() && password.length() < 6) { showError("Password minimal 6 karakter jika diisi."); return; }
        if (!pin.isEmpty() && !pin.matches("\\d{4}|\\d{6}")) { showError("PIN harus 4 atau 6 digit angka jika diisi."); return; }

        hideError();
        boolean success = false;

        if (isEditMode && userToEdit != null) {
            // Cek apakah username diubah dan apakah username baru sudah ada (milik user lain)
            if (!userToEdit.getUsername().equalsIgnoreCase(username) && userService.isUsernameTaken(username)) {
                showError("Username '" + username + "' sudah digunakan oleh pengguna lain.");
                return;
            }

            userToEdit.setUsername(username);
            if (!password.isEmpty()) { // Hanya update password jika diisi
                userToEdit.setPasswordHash(PasswordUtil.hashPassword(password));
            }
            if (!pin.isEmpty()) { // Hanya update PIN jika diisi
                userToEdit.setPinHash(PasswordUtil.hashPin(pin));
            }
            // Admin tidak boleh mengubah role super admin "admin" menjadi "user"
            if(userToEdit.getUsername().equals("admin") && role.equals("user") && userToEdit.getRole().equals("admin")){
                showError("Role super admin tidak dapat diubah menjadi user.");
                return;
            }
            // Admin tidak boleh mengedit dirinya sendiri menjadi role user jika dia adalah admin yang sedang login
            if (userToEdit.getUserId() == SessionManager.getCurrentUserId() &&
                    SessionManager.getCurrentUserRole().equals("admin") && role.equals("user")) {
                showError("Anda tidak dapat mengubah role akun Anda sendiri menjadi 'user'.");
                return;
            }

            userToEdit.setRole(role);
            success = userService.updateUserByAdmin(userToEdit);

        } else { // Mode Tambah Baru
            User createdUser = userService.createUserByAdmin(username, password, pin, role);
            if (createdUser != null) {
                success = true;
            } else {
                // userService.createUserByAdmin akan print error spesifik jika username sudah ada
                showError("Gagal membuat pengguna. Username mungkin sudah ada.");
            }
        }

        if (success) {
            if (dialogHandler != null) {
                dialogHandler.onUserSaved();
            }
            closeDialog();
        } else if (!isEditMode) { // Hanya tampilkan error umum jika tambah baru gagal dan belum ada pesan spesifik
            // (karena userService.createUserByAdmin sudah print error jika username ada)
            // showError("Gagal menyimpan pengguna. Periksa log service.");
        }
    }

    @FXML
    private void handleCancelUserAdminAction(ActionEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) saveUserAdminButton.getScene().getWindow(); // Ambil dari tombol simpan
        stage.close();
    }

    private void showError(String message) {
        errorUserDialogLabel.setText(message);
        errorUserDialogLabel.setVisible(true);
        errorUserDialogLabel.setManaged(true);
    }

    private void hideError() {
        errorUserDialogLabel.setText("");
        errorUserDialogLabel.setVisible(false);
        errorUserDialogLabel.setManaged(false);
    }
}