package org.neracaku.neracaku.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert; // Import Alert
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType; // Import ButtonType
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
// import javafx.stage.StageStyle; // Tidak kita gunakan di sini
import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.services.CategoryService; // Import CategoryService
import org.neracaku.neracaku.utils.SessionManager; // Import SessionManager (mungkin dibutuhkan untuk delete)
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
// import java.net.URL; // Tidak dibutuhkan jika FXMLLoader.load() menggunakan path absolut dari resources
import java.util.List; // Import List
import java.util.Optional; // Import Optional

public class CategoryController {

    @FXML
    private Button addCategoryButton;

    @FXML
    private TableView<Category> categoryTableView;

    @FXML
    private TableColumn<Category, String> nameColumn;

    @FXML
    private TableColumn<Category, String> typeColumn;

    @FXML
    private TableColumn<Category, Void> actionsColumn;

    private CategoryService categoryService; // Deklarasi CategoryService
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("CategoryController initialized.");
        categoryService = new CategoryService(); // Inisialisasi CategoryService

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        // Ubah tipe agar menampilkan "Pemasukan" / "Pengeluaran" dengan huruf kapital
        typeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Kapitalisasi huruf pertama
                    setText(item.substring(0, 1).toUpperCase() + item.substring(1));
                }
            }
        });


        setupActionsColumn();

        categoryTableView.setItems(categoryList);
        loadCategories();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("", new FontIcon("fas-edit"));
            private final Button deleteButton = new Button("", new FontIcon("fas-trash-alt"));
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.getStyleClass().addAll("button-icon-info");
                deleteButton.getStyleClass().addAll("button-icon-danger");

                editButton.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    // Hanya user yang bisa edit kategori custom miliknya, atau admin bisa edit kategori default/custom
                    if (!category.isIsDefault() && category.getUserId() != null && category.getUserId().equals(SessionManager.getCurrentUserId())) {
                        handleEditCategoryAction(category);
                    } else if (category.isIsDefault() && "admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
                        handleEditCategoryAction(category); // Admin bisa edit default
                    } else if (!category.isIsDefault() && "admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
                        handleEditCategoryAction(category); // Admin bisa edit kategori user lain (jika diinginkan)
                    }
                    else {
                        showAlert(Alert.AlertType.WARNING, "Aksi Ditolak", "Anda tidak memiliki hak untuk mengedit kategori ini.");
                    }
                });

                deleteButton.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    if (!category.isIsDefault() && category.getUserId() != null && category.getUserId().equals(SessionManager.getCurrentUserId())) {
                        handleDeleteCategoryAction(category);
                    } else if (category.isIsDefault() && "admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
                        handleDeleteCategoryAction(category); // Admin bisa hapus default
                    } else if (!category.isIsDefault() && "admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
                        // Admin mungkin tidak seharusnya menghapus kategori user lain tanpa konfirmasi/logika khusus
                        showAlert(Alert.AlertType.WARNING, "Aksi Ditolak", "Admin tidak dapat menghapus kategori spesifik user lain dari sini.");
                    }
                    else {
                        showAlert(Alert.AlertType.WARNING, "Aksi Ditolak", "Anda tidak memiliki hak untuk menghapus kategori ini.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Hanya tampilkan tombol jika ada hak akses (misalnya)
                    // Category category = getTableView().getItems().get(getIndex());
                    // if (!category.isIsDefault() || "admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
                    //     setGraphic(pane);
                    // } else {
                    //     setGraphic(null); // Sembunyikan tombol jika user biasa & kategori default
                    // }
                    // Untuk sekarang, tampilkan saja semua tombol, validasi ada di action handler
                    setGraphic(pane);
                }
            }
        });
    }

    private void loadCategories() {
        categoryList.clear(); // Kosongkan list sebelum memuat data baru
        System.out.println("Memuat kategori dari database...");
        List<Category> fetchedCategories = categoryService.getAllCategoriesForDisplay();
        if (fetchedCategories != null) {
            categoryList.addAll(fetchedCategories);
            System.out.println(fetchedCategories.size() + " kategori dimuat.");
        } else {
            System.out.println("Tidak ada kategori yang dimuat atau terjadi error.");
        }
    }

    @FXML
    void handleAddCategoryAction(ActionEvent event) {
        System.out.println("Tombol Tambah Kategori diklik.");
        openCategoryDialog(null);
    }

    private void handleEditCategoryAction(Category category) {
        System.out.println("Edit kategori: " + category.getName());
        openCategoryDialog(category);
    }

    private void handleDeleteCategoryAction(Category category) {
        System.out.println("Mencoba menghapus kategori: " + category.getName());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus Kategori");
        alert.setHeaderText("Hapus Kategori: " + category.getName());
        alert.setContentText("Anda yakin ingin menghapus kategori '" + category.getName() + "'?\n" +
                (category.isIsDefault() ? "Ini adalah kategori default sistem." : "Ini adalah kategori custom Anda."));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted;
            if (category.isIsDefault() && "admin".equalsIgnoreCase(SessionManager.getCurrentUserRole())) {
                deleted = categoryService.deleteDefaultForAdmin(category.getCategoryId());
            } else if (!category.isIsDefault() && category.getUserId() != null && category.getUserId().equals(SessionManager.getCurrentUserId())) {
                deleted = categoryService.deleteUserCategory(category.getCategoryId());
            } else {
                showAlert(Alert.AlertType.ERROR, "Gagal Hapus", "Anda tidak memiliki hak untuk menghapus kategori ini.");
                return;
            }

            if (deleted) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Kategori '" + category.getName() + "' berhasil dihapus.");
                loadCategories(); // Muat ulang daftar kategori
            } else {
                showAlert(Alert.AlertType.ERROR, "Gagal Hapus", "Gagal menghapus kategori '" + category.getName() + "'. Kategori mungkin masih digunakan atau terjadi kesalahan lain.");
            }
        } else {
            System.out.println("Penghapusan kategori dibatalkan.");
        }
    }

    private void openCategoryDialog(Category categoryToEdit) {
        try {
            // Path FXML harus dimulai dengan '/' jika dari root classpath (resources)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/neracaku/neracaku/fxml/AddEditCategoryDialog.fxml"));
            Parent root = loader.load();

            AddEditCategoryDialogController controller = loader.getController();
            if (categoryToEdit != null) {
                controller.setCategoryToEdit(categoryToEdit);
            }
            // controller.setCategoryController(this); // Uncomment jika dialog perlu callback

            Stage dialogStage = new Stage();
            dialogStage.setTitle(categoryToEdit == null ? "Tambah Kategori Baru" : "Edit Kategori");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(addCategoryButton.getScene().getWindow()); // Set owner stage

            Scene scene = new Scene(root);
            // Jika styles.css sudah di-link di FXML dialog, baris ini tidak perlu.
            // Jika belum, tambahkan:
            scene.getStylesheets().add(getClass().getResource("/org/neracaku/neracaku/css/styles.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            // Setelah dialog ditutup, muat ulang kategori
            // karena bisa jadi ada penambahan atau pengeditan
            loadCategories();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error Dialog", "Gagal membuka dialog kategori: " + e.getMessage());
        }
    }

    // Helper method untuk menampilkan alert
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // Tidak ada header
        alert.setContentText(message);
        alert.showAndWait();
    }
}