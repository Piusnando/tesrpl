package org.neracaku.neracaku.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.services.CategoryService; // Import CategoryService
import org.neracaku.neracaku.utils.SessionManager; // Import SessionManager

public class AddEditCategoryDialogController {

    @FXML
    private Label dialogTitleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private ChoiceBox<String> typeChoiceBox;

    @FXML
    private Label errorLabelDialog;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private CategoryService categoryService; // Deklarasi CategoryService
    private Category categoryToEdit;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        categoryService = new CategoryService(); // Inisialisasi CategoryService
        typeChoiceBox.setItems(FXCollections.observableArrayList("Pemasukan", "Pengeluaran"));
        typeChoiceBox.setValue("Pengeluaran"); // Set default value
        hideError();
        System.out.println("AddEditCategoryDialogController initialized.");
    }

    public void setCategoryToEdit(Category category) {
        this.categoryToEdit = category;
        this.isEditMode = true;

        dialogTitleLabel.setText("Edit Kategori");
        nameField.setText(category.getName());
        if ("pemasukan".equalsIgnoreCase(category.getType())) {
            typeChoiceBox.setValue("Pemasukan");
        } else if ("pengeluaran".equalsIgnoreCase(category.getType())) {
            typeChoiceBox.setValue("Pengeluaran");
        }
        saveButton.setText("Update"); // Ubah teks tombol simpan
    }

    @FXML
    private void handleSaveAction(ActionEvent event) {
        String name = nameField.getText().trim();
        String selectedTypeDisplay = typeChoiceBox.getValue(); // "Pemasukan" atau "Pengeluaran"

        if (name.isEmpty()) {
            showError("Nama kategori tidak boleh kosong.");
            nameField.requestFocus();
            return;
        }
        if (selectedTypeDisplay == null || selectedTypeDisplay.isEmpty()) {
            showError("Tipe kategori harus dipilih.");
            typeChoiceBox.requestFocus();
            return;
        }

        hideError();
        String typeValueDb = selectedTypeDisplay.toLowerCase(); // Untuk disimpan ke DB ("pemasukan" atau "pengeluaran")
        boolean success;

        if (isEditMode && categoryToEdit != null) {
            System.out.println("Menyimpan perubahan untuk kategori ID: " + categoryToEdit.getCategoryId());
            // Pastikan hanya properti yang boleh diubah yang di-set
            // Misal, userId dan isDefault tidak diubah dari dialog ini untuk kategori user
            categoryToEdit.setName(name);
            categoryToEdit.setType(typeValueDb);
            // Jika ini kategori default yg diedit admin, mungkin ada logika khusus
            // Tapi untuk kategori user, isDefault tetap false dan userId tetap sama

            success = categoryService.updateExistingCategory(categoryToEdit);
            if (!success) {
                showError("Gagal menyimpan perubahan kategori. Mungkin nama & tipe sudah ada?");
            }
        } else {
            System.out.println("Menyimpan kategori baru: " + name + ", Tipe: " + typeValueDb);
            Category newCategory = new Category();
            newCategory.setName(name);
            newCategory.setType(typeValueDb);
            newCategory.setIsDefault(false); // Kategori dari user selalu bukan default
            // UserId akan di-set oleh service berdasarkan SessionManager jika !isDefault

            success = categoryService.saveNewCategory(newCategory);
            if (!success) {
                showError("Gagal menyimpan kategori baru. Mungkin nama & tipe sudah ada untuk Anda?");
            }
        }

        if (success) {
            // Jika categoryController diset, bisa panggil method refresh di sini
            // if (categoryController != null) {
            //     categoryController.loadCategories();
            // }
            closeDialog();
        }
    }

    @FXML
    private void handleCancelAction(ActionEvent event) {
        System.out.println("Batal diklik.");
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabelDialog.setText(message);
        errorLabelDialog.setVisible(true);
        errorLabelDialog.setManaged(true);
    }

    private void hideError() {
        errorLabelDialog.setText("");
        errorLabelDialog.setVisible(false);
        errorLabelDialog.setManaged(false);
    }
}