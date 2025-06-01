package org.neracaku.neracaku.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image; // Import Image
import javafx.scene.image.ImageView; // Import ImageView
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.models.Transaction;
import org.neracaku.neracaku.services.CategoryService;
import org.neracaku.neracaku.services.TransactionService;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AddEditTransactionDialogController {

    @FXML
    private Label dialogTitleLabel;
    @FXML
    private ChoiceBox<String> typeChoiceBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<Category> categoryComboBox;
    @FXML
    private TextField amountField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Button chooseImageButton;
    @FXML
    private Label imagePathLabel;
    @FXML
    private ImageView receiptImageView;
    @FXML
    private Label errorLabelDialog;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private TransactionService transactionService;
    private CategoryService categoryService;

    private Transaction transactionToProcess;
    private boolean isEditMode = false;
    private boolean isViewMode = false;
    private File selectedImageFile;

    public interface TransactionDialogHandler {
        void onSave();
    }
    private TransactionDialogHandler dialogHandler;

    public void setDialogHandler(TransactionDialogHandler handler) {
        this.dialogHandler = handler;
    }

    @FXML
    public void initialize() {
        transactionService = new TransactionService();
        categoryService = new CategoryService();

        typeChoiceBox.setItems(FXCollections.observableArrayList("Pemasukan", "Pengeluaran"));
        typeChoiceBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (!isViewMode) loadCategoriesByType(newVal); // Jangan reload jika view mode & kategori sudah diset
                }
        );

        categoryComboBox.setConverter(new javafx.util.StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category == null ? null : category.getName();
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        });

        datePicker.setValue(LocalDate.now()); // Default ke hari ini

        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!isViewMode && !newValue.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(newValue.replaceAll("[^\\d.]", ""));
                if (amountField.getText().chars().filter(ch -> ch == '.').count() > 1) {
                    amountField.setText(oldValue);
                }
            }
        });

        hideError();
        receiptImageView.setVisible(false); // Sembunyikan image view awalnya
        receiptImageView.setManaged(false);
        System.out.println("AddEditTransactionDialogController initialized.");
    }

    private void loadCategoriesByType(String transactionTypeDisplay) {
        if (transactionTypeDisplay == null || transactionTypeDisplay.isEmpty()) {
            categoryComboBox.setItems(FXCollections.observableArrayList());
            return;
        }
        String typeDbValue = transactionTypeDisplay.toLowerCase();
        List<Category> categories = categoryService.getCategoriesByTypeForCurrentUser(typeDbValue);
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));

        if (!categories.isEmpty()) {
            if ((isEditMode || isViewMode) && transactionToProcess != null && transactionToProcess.getCategoryId() != 0) {
                Optional<Category> catToSelect = categories.stream()
                        .filter(c -> c.getCategoryId() == transactionToProcess.getCategoryId())
                        .findFirst();
                if (catToSelect.isPresent()) {
                    categoryComboBox.setValue(catToSelect.get());
                } else if (!isViewMode) { // Jika edit dan kategori lama tidak ada di list baru, pilih yg pertama
                    categoryComboBox.getSelectionModel().selectFirst();
                }
            } else if (!isViewMode) { // Hanya set default jika bukan view mode atau sudah ada value
                categoryComboBox.getSelectionModel().selectFirst();
            }
        } else {
            categoryComboBox.setValue(null);
        }
    }

    // Dipanggil untuk mode Tambah Baru
    public void setNewTransactionMode() {
        this.isEditMode = false;
        this.isViewMode = false;
        this.transactionToProcess = new Transaction();

        dialogTitleLabel.setText("Tambah Transaksi Baru");
        saveButton.setText("Simpan");
        typeChoiceBox.setValue("Pengeluaran"); // Default ke pengeluaran
        loadCategoriesByType("Pengeluaran"); // Muat kategori pengeluaran
        clearFields();
        enableFields();
    }

    // Dipanggil untuk mode Edit
    public void setTransactionToEdit(Transaction transaction) {
        this.transactionToProcess = transaction;
        this.isEditMode = true;
        this.isViewMode = false;

        dialogTitleLabel.setText("Edit Transaksi");
        saveButton.setText("Update");
        populateFields(transaction);
        enableFields();
    }

    // Dipanggil untuk mode Lihat Detail
    public void setTransactionToView(Transaction transaction) {
        this.transactionToProcess = transaction;
        this.isEditMode = false;
        this.isViewMode = true;

        dialogTitleLabel.setText("Detail Transaksi");
        saveButton.setText("Tutup");

        populateFields(transaction);
        disableFields();
        displayReceiptImage(transaction.getImagePath());
    }


    private void populateFields(Transaction transaction) {
        if ("pemasukan".equalsIgnoreCase(transaction.getType())) {
            typeChoiceBox.setValue("Pemasukan");
        } else {
            typeChoiceBox.setValue("Pengeluaran");
        }
        // loadCategoriesByType akan dipanggil oleh listener typeChoiceBox

        datePicker.setValue(transaction.getTransactionDate());
        amountField.setText(String.format(Locale.US, "%.0f", transaction.getAmount())); // Format tanpa desimal jika integer
        descriptionArea.setText(transaction.getDescription());

        // Muat kategori setelah tipe diset, lalu pilih kategori yang benar
        loadCategoriesByType(typeChoiceBox.getValue());
        if (transaction.getCategoryId() != 0) {
            Optional<Category> catToSelect = categoryComboBox.getItems().stream()
                    .filter(c -> c.getCategoryId() == transaction.getCategoryId())
                    .findFirst();
            catToSelect.ifPresent(categoryComboBox::setValue);
        }

        if (transaction.getImagePath() != null && !transaction.getImagePath().isEmpty()) {
            selectedImageFile = new File(transaction.getImagePath()); // Simpan sebagai File jika path valid
            imagePathLabel.setText(selectedImageFile.exists() ? selectedImageFile.getName() : "(File tidak ditemukan)");
        } else {
            imagePathLabel.setText("(Tidak ada file dipilih)");
            selectedImageFile = null;
        }
    }

    private void clearFields() {
        // typeChoiceBox sudah di-set default
        datePicker.setValue(LocalDate.now());
        categoryComboBox.getSelectionModel().clearSelection();
        amountField.clear();
        descriptionArea.clear();
        imagePathLabel.setText("(Tidak ada file dipilih)");
        selectedImageFile = null;
        receiptImageView.setImage(null);
        receiptImageView.setVisible(false);
        receiptImageView.setManaged(false);
    }

    private void enableFields() {
        typeChoiceBox.setDisable(false);
        datePicker.setDisable(false);
        categoryComboBox.setDisable(false);
        amountField.setEditable(true);
        descriptionArea.setEditable(true);
        chooseImageButton.setDisable(false);
        chooseImageButton.setVisible(true);
        chooseImageButton.setManaged(true);
        // imagePathLabel akan diupdate oleh chooseImageButton atau populateFields
    }

    private void disableFields() {
        typeChoiceBox.setDisable(true);
        datePicker.setDisable(true);
        categoryComboBox.setDisable(true);
        amountField.setEditable(false);
        descriptionArea.setEditable(false);
        chooseImageButton.setDisable(true); // Tombol pilih file juga nonaktif
        chooseImageButton.setVisible(false);
        chooseImageButton.setManaged(false);
    }

    private void displayReceiptImage(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists() && imageFile.isFile()) {
                    Image image = new Image(imageFile.toURI().toString(), 200, 150, true, true); // Max width 200, max height 150, preserve ratio, smooth
                    receiptImageView.setImage(image);
                    receiptImageView.setVisible(true);
                    receiptImageView.setManaged(true);
                } else {
                    System.err.println("File gambar nota tidak ditemukan di path: " + imagePath);
                    imagePathLabel.setText("(Gambar tidak ditemukan)");
                    receiptImageView.setImage(null);
                    receiptImageView.setVisible(false);
                    receiptImageView.setManaged(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imagePathLabel.setText("(Gagal memuat gambar)");
                receiptImageView.setImage(null);
                receiptImageView.setVisible(false);
                receiptImageView.setManaged(false);
            }
        } else {
            receiptImageView.setImage(null);
            receiptImageView.setVisible(false);
            receiptImageView.setManaged(false);
        }
    }


    @FXML
    private void handleChooseImageAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Gambar Nota");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("File Gambar", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Semua File", "*.*")
        );
        File file = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) { // 5 MB
                showError("Ukuran file gambar tidak boleh melebihi 5MB.");
                selectedImageFile = null;
                imagePathLabel.setText("(File terlalu besar)");
                return;
            }
            selectedImageFile = file;
            imagePathLabel.setText(selectedImageFile.getName());
            hideError();
            displayReceiptImage(selectedImageFile.getAbsolutePath()); // Langsung tampilkan preview jika dipilih
        }
    }

    @FXML
    private void handleSaveAction(ActionEvent event) {
        if (isViewMode) {
            closeDialog();
            return;
        }

        hideError();
        if (typeChoiceBox.getValue() == null) { showError("Jenis transaksi harus dipilih."); return; }
        if (datePicker.getValue() == null) { showError("Tanggal transaksi harus dipilih."); return; }
        if (categoryComboBox.getValue() == null) { showError("Kategori harus dipilih."); return; }
        if (amountField.getText().trim().isEmpty()) { showError("Jumlah tidak boleh kosong."); return; }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", "")); // Hapus koma jika ada
            if (amount <= 0) { showError("Jumlah harus lebih besar dari nol."); return; }
        } catch (NumberFormatException e) {
            showError("Format jumlah tidak valid."); return;
        }

        LocalDate date = datePicker.getValue();
        Category selectedCategory = categoryComboBox.getValue();
        String description = descriptionArea.getText().trim();
        String imagePathToSave = null;

        if (selectedImageFile != null) {
            imagePathToSave = selectedImageFile.getAbsolutePath();
            System.out.println("Path gambar yang akan disimpan: " + imagePathToSave);
        } else if (isEditMode && transactionToProcess != null && transactionToProcess.getImagePath() != null) {
            imagePathToSave = transactionToProcess.getImagePath(); // Pertahankan path lama jika tidak ada file baru
        }

        boolean success;
        // transactionToProcess sudah diinisialisasi di setNewTransactionMode atau setTransactionToEdit
        transactionToProcess.setTransactionDate(date);
        transactionToProcess.setCategoryId(selectedCategory.getCategoryId());
        transactionToProcess.setAmount(amount);
        transactionToProcess.setDescription(description);
        transactionToProcess.setImagePath(imagePathToSave);
        // Tipe dan UserId akan di-set oleh TransactionService

        if (isEditMode) {
            System.out.println("Mengupdate transaksi ID: " + transactionToProcess.getTransactionId());
            success = transactionService.updateExistingTransaction(transactionToProcess);
        } else {
            System.out.println("Menyimpan transaksi baru...");
            success = transactionService.saveNewTransaction(transactionToProcess);
        }

        if (success) {
            System.out.println("Transaksi berhasil disimpan/diupdate.");
            if (dialogHandler != null) {
                dialogHandler.onSave();
            }
            closeDialog();
        } else {
            showError("Gagal menyimpan transaksi. Periksa pesan error di console service.");
        }
    }

    @FXML
    private void handleCancelAction(ActionEvent event) {
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