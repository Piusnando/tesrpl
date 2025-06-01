package org.neracaku.neracaku.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene; // Import Scene
import javafx.scene.control.Alert; // Import Alert
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality; // Import Modality
import javafx.stage.Stage;
import org.neracaku.neracaku.models.User; // Import User jika belum
import org.neracaku.neracaku.utils.NavigationUtil;
import org.neracaku.neracaku.utils.SessionManager;

import java.io.IOException;
import java.net.URL;

public class MainViewController {

    @FXML
    private BorderPane mainBorderPane; // Jika Anda memberi fx:id ke BorderPane root

    @FXML
    private VBox sidebarPane;

    @FXML
    private Button quickCreateButton;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button transactionsButton;

    @FXML
    private Button categoriesButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button adminUserMgmtButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private AnchorPane contentAreaPane; // Kontainer untuk memuat FXML lain

    private Button currentActiveButton = null;


    @FXML
    public void initialize() {
        System.out.println("MainViewController initialized.");
        setupUserInfo();
        handleDashboardAction(null);
    }

    private void setupUserInfo() {
        if (SessionManager.isLoggedIn()) {
            User currentUser = SessionManager.getCurrentUser();
            usernameLabel.setText(currentUser.getUsername());
            userRoleLabel.setText(currentUser.getRole().substring(0, 1).toUpperCase() + currentUser.getRole().substring(1)); // Capitalize role

            // Tampilkan tombol manajemen user jika admin
            if ("admin".equalsIgnoreCase(currentUser.getRole())) {
                adminUserMgmtButton.setVisible(true);
                adminUserMgmtButton.setManaged(true);
            }
        } else {
            // Jika tidak ada sesi, idealnya kembali ke login
            // Ini seharusnya tidak terjadi jika alur navigasi sudah benar
            handleLogoutAction(null);
        }
    }

    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active");
        }
        if (button != null) {
            button.getStyleClass().add("active");
        }
        currentActiveButton = button;
    }

    private void loadContent(String fxmlPath) {
        try {
            // Pastikan path resource benar
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Tidak dapat menemukan FXML konten: " + fxmlPath);
                // Tampilkan pesan error di contentAreaPane
                Label errorLabel = new Label("Error: Konten tidak ditemukan (" + fxmlPath + ")");
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                contentAreaPane.getChildren().setAll(errorLabel);
                return;
            }
            Parent newContent = FXMLLoader.load(fxmlUrl);
            contentAreaPane.getChildren().setAll(newContent); // Ganti semua anak dari contentAreaPane

            // Jika contentAreaPane adalah AnchorPane, Anda mungkin perlu mengatur anchor untuk newContent
            if (newContent != null && contentAreaPane instanceof AnchorPane) {
                AnchorPane.setTopAnchor(newContent, 0.0);
                AnchorPane.setBottomAnchor(newContent, 0.0);
                AnchorPane.setLeftAnchor(newContent, 0.0);
                AnchorPane.setRightAnchor(newContent, 0.0);
            }

        } catch (IOException e) {
            System.err.println("Gagal memuat konten FXML '" + fxmlPath + "': " + e.getMessage());
            e.printStackTrace();
            Label errorLabel = new Label("Error memuat konten: " + e.getMessage());
            contentAreaPane.getChildren().setAll(errorLabel);
        }
    }

    private void openQuickAddTransactionDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/neracaku/neracaku/fxml/AddEditTransactionDialog.fxml"));
            Parent root = loader.load();

            AddEditTransactionDialogController dialogController = loader.getController();
            dialogController.setNewTransactionMode(); // PENTING: Set ke mode tambah baru

            // Handler jika Anda ingin melakukan sesuatu setelah dialog ditutup (misal, refresh dashboard)
            dialogController.setDialogHandler(() -> {
                System.out.println("Dialog tambah cepat ditutup dari MainView, me-refresh dashboard...");
                handleDashboardAction(null); // Atau method refresh dashboard yang lebih spesifik
            });

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Tambah Transaksi Baru (Cepat)");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            // Set owner stage ke stage utama MainView
            Stage ownerStage = (Stage) mainBorderPane.getScene().getWindow();
            dialogStage.initOwner(ownerStage);

            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/org/neracaku/neracaku/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Gagal memuat styles.css untuk dialog transaksi cepat.");
            }
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal membuka dialog transaksi: " + e.getMessage());
            alert.showAndWait();
        }
    }


    @FXML
    void handleQuickCreateAction(ActionEvent event) {
        System.out.println("Quick Create diklik");
//        setActiveButton(quickCreateButton);
        // TODO: Buka dialog tambah transaksi
        openQuickAddTransactionDialog();
    }

    @FXML
    void handleDashboardAction(ActionEvent event) {
        System.out.println("Dashboard diklik");
        setActiveButton(dashboardButton);
        loadContent("/org/neracaku/neracaku/fxml/DashboardView.fxml");
    }

    @FXML
    void handleTransactionsAction(ActionEvent event) {
        System.out.println("Transaksi diklik");
        setActiveButton(transactionsButton);
        // Ganti dengan path FXML yang benar untuk daftar transaksi
        loadContent("/org/neracaku/neracaku/fxml/TransactionListView.fxml"); // Buat FXML ini nanti
    }

    @FXML
    void handleCategoriesAction(ActionEvent event) {
        System.out.println("Kategori diklik");
        setActiveButton(categoriesButton);
        loadContent("/org/neracaku/neracaku/fxml/CategoryView.fxml");
    }

    @FXML
    void handleReportsAction(ActionEvent event) {
        System.out.println("Laporan diklik");
        setActiveButton(reportsButton);
        loadContent("/org/neracaku/neracaku/fxml/ReportView.fxml");
    }

    @FXML
    void handleAdminUserMgmtAction(ActionEvent event) {
        System.out.println("Manajemen User diklik");
        setActiveButton(adminUserMgmtButton);
        loadContent("/org/neracaku/neracaku/fxml/AdminUserManagementView.fxml");
    }

    @FXML
    void handleLogoutAction(ActionEvent event) {
        System.out.println("Logout diklik");
        SessionManager.clearSession();
        Stage currentStage = (Stage) logoutButton.getScene().getWindow(); // Cara lain mendapatkan stage
        // Atau jika event tidak null: Stage currentStage = NavigationUtil.getStageFromEvent(event);
        if (currentStage != null) {
            NavigationUtil.navigateTo("/org/neracaku/neracaku/fxml/LoginView.fxml", "Neracaku - Login", currentStage);
        }
    }
}