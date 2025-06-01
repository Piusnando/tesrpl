package org.neracaku.neracaku.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.neracaku.neracaku.models.User;
import org.neracaku.neracaku.services.UserService;
import org.neracaku.neracaku.utils.SessionManager; // Untuk mencegah admin hapus diri sendiri

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AdminUserManagementController {

    @FXML
    private Button addUserButton;
    @FXML
    private TableView<User> userTableView;
    @FXML
    private TableColumn<User, Integer> userIdColumn;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, String> createdAtColumn;
    @FXML
    private TableColumn<User, Void> actionsUserColumn;

    private UserService userService;
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML
    public void initialize() {
        System.out.println("AdminUserManagementController initialized.");
        userService = new UserService();

        setupUserTableColumns();
        userTableView.setItems(userList);
        loadUsers();
    }

    private void setupUserTableColumns() {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(capitalize(cellData.getValue().getRole()))
        );
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime createdAt = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt != null ? createdAt.format(dateTimeFormatter) : "N/A");
        });

        actionsUserColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("", new FontIcon("fas-edit"));
            private final Button deleteButton = new Button("", new FontIcon("fas-user-times")); // Ikon berbeda untuk delete user
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.getStyleClass().addAll("button-icon-info");
                deleteButton.getStyleClass().addAll("button-icon-danger");

                editButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUserAction(user);
                });

                deleteButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    // Admin tidak boleh menghapus dirinya sendiri
                    if (user.getUserId() == SessionManager.getCurrentUserId()) {
                        showAlert(Alert.AlertType.WARNING, "Aksi Ditolak", "Anda tidak dapat menghapus akun Anda sendiri.");
                        return;
                    }
                    handleDeleteUserAction(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Tombol hapus dinonaktifkan jika user adalah admin yang sedang login
                    User user = getTableView().getItems().get(getIndex());
                    deleteButton.setDisable(user.getUserId() == SessionManager.getCurrentUserId());
                    setGraphic(pane);
                }
            }
        });
    }

    private void loadUsers() {
        userList.clear();
        List<User> fetchedUsers = userService.getAllUsersForAdmin();
        if (fetchedUsers != null) {
            userList.addAll(fetchedUsers);
            System.out.println(fetchedUsers.size() + " pengguna dimuat ke tabel.");
        } else {
            System.out.println("Tidak ada pengguna yang dimuat atau terjadi error.");
        }
    }

    @FXML
    void handleAddUserAction(ActionEvent event) {
        System.out.println("Tombol Tambah Pengguna diklik.");
        openUserDialog(null); // null berarti mode tambah baru
    }

    private void handleEditUserAction(User user) {
        System.out.println("Edit pengguna: " + user.getUsername());
        openUserDialog(user); // user yang ada berarti mode edit
    }

    private void handleDeleteUserAction(User user) {
        System.out.println("Mencoba menghapus pengguna: " + user.getUsername() + " (ID: " + user.getUserId() + ")");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus Pengguna");
        alert.setHeaderText("Hapus Pengguna: " + user.getUsername());
        alert.setContentText("Anda yakin ingin menghapus pengguna '" + user.getUsername() + "'?\n" +
                "Semua data transaksi dan kategori milik pengguna ini juga akan dihapus PERMANEN!");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (userService.deleteUserByAdmin(user.getUserId())) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Pengguna '" + user.getUsername() + "' berhasil dihapus.");
                loadUsers(); // Muat ulang daftar pengguna
            } else {
                showAlert(Alert.AlertType.ERROR, "Gagal Hapus", "Gagal menghapus pengguna '" + user.getUsername() + "'.");
            }
        } else {
            System.out.println("Penghapusan pengguna dibatalkan.");
        }
    }

    private void openUserDialog(User userToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/neracaku/neracaku/fxml/AddEditUserByAdminDialog.fxml"));
            Parent root = loader.load();

            AddEditUserByAdminDialogController controller = loader.getController();
            if (userToEdit != null) {
                controller.setUserToEdit(userToEdit);
            } else {
                controller.setNewUserMode();
            }

            // Set handler untuk refresh tabel setelah dialog ditutup & simpan berhasil
            controller.setDialogHandler(this::loadUsers);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(userToEdit == null ? "Tambah Pengguna Baru (Admin)" : "Edit Pengguna (Admin)");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            // Set owner stage ke stage utama atau stage dari tombol addUserButton
            Stage ownerStage = (Stage) addUserButton.getScene().getWindow();
            dialogStage.initOwner(ownerStage);

            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/org/neracaku/neracaku/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error Dialog", "Gagal membuka dialog pengguna: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}