package org.neracaku.neracaku.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.models.Transaction;
import org.neracaku.neracaku.services.CategoryService;
import org.neracaku.neracaku.services.TransactionService;
// SessionManager tidak lagi dibutuhkan di sini secara langsung untuk delete, karena service yang handle

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TransactionListController {

    // ... (Deklarasi @FXML Anda yang sudah ada tetap sama) ...
    @FXML private Button addTransactionButton;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<Category> filterCategoryComboBox;
    @FXML private TextField searchKeywordField;
    @FXML private Button applyFilterButton;
    @FXML private Button resetFilterButton;
    @FXML private TableView<Transaction> transactionTableView;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, String> categoryColumn;
    @FXML private TableColumn<Transaction, String> descriptionColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, String> amountColumn;
    @FXML private TableColumn<Transaction, Void> actionsColumn;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;
    @FXML private Label netBalanceLabel;

    private TransactionService transactionService;
    private CategoryService categoryService;
    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryFilterList = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    @FXML
    public void initialize() {
        // ... (Kode initialize Anda sudah baik) ...
        System.out.println("TransactionListController initialized.");
        transactionService = new TransactionService();
        categoryService = new CategoryService();

        setupTableColumns();
        setupFilters();

        transactionTableView.setItems(transactionList);
        loadTransactions();
    }

    private void setupTableColumns() {
        // ... (Kode setupTableColumns Anda sudah baik) ...
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTransactionDate().format(dateFormatter))
        );
        categoryColumn.setCellValueFactory(cellData -> {
            Category category = categoryService.getCategoryById(cellData.getValue().getCategoryId()).orElse(null);
            return new SimpleStringProperty(category != null ? category.getName() : "N/A");
        });
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(capitalize(cellData.getValue().getType()))
        );
        amountColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getAmount()))
        );
        // amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;"); // Alignment dari FXML/CSS

        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("", new FontIcon("fas-eye")); // Tombol View
            private final Button editButton = new Button("", new FontIcon("fas-edit"));
            private final Button deleteButton = new Button("", new FontIcon("fas-trash-alt"));
            // Atur urutan tombol di HBox
            private final HBox pane = new HBox(5, viewButton, editButton, deleteButton);


            {
                viewButton.getStyleClass().addAll("button-icon-secondary"); // Buat style class ini jika perlu
                editButton.getStyleClass().addAll("button-icon-info");
                deleteButton.getStyleClass().addAll("button-icon-danger");

                viewButton.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    handleViewDetailAction(transaction);
                });
                editButton.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    handleEditTransactionAction(transaction);
                });
                deleteButton.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    handleDeleteTransactionAction(transaction);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // ... (setupFilters, loadTransactions, updateSummaryLabels, handleAddTransactionAction sudah baik) ...
    private void setupFilters() {
        List<Category> allCategories = categoryService.getAllCategoriesForDisplay();
        categoryFilterList.add(null);
        categoryFilterList.addAll(allCategories);
        filterCategoryComboBox.setItems(categoryFilterList);
        filterCategoryComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Category category) { return category == null ? "Semua Kategori" : category.getName(); }
            @Override public Category fromString(String string) { return null; }
        });
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
    }

    private void loadTransactions() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        Category selectedCategory = filterCategoryComboBox.getValue();
        Integer categoryId = (selectedCategory != null) ? selectedCategory.getCategoryId() : null;
        String keyword = searchKeywordField.getText();
        List<Transaction> fetchedTransactions = transactionService.getFilteredTransactionsForCurrentUser(startDate, endDate, categoryId, keyword);
        transactionList.setAll(fetchedTransactions);
        System.out.println(fetchedTransactions.size() + " transaksi dimuat ke tabel.");
        updateSummaryLabels(fetchedTransactions);
    }
    private void updateSummaryLabels(List<Transaction> transactions) {
        double income = transactions.stream().filter(t -> "pemasukan".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double expense = transactions.stream().filter(t -> "pengeluaran".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        totalIncomeLabel.setText(currencyFormatter.format(income));
        totalExpenseLabel.setText(currencyFormatter.format(expense));
        netBalanceLabel.setText(currencyFormatter.format(income - expense));
        if ((income - expense) < 0) { netBalanceLabel.setStyle("-fx-text-fill: rgb(255, 91, 91); -fx-font-weight: bold;"); }
        else { netBalanceLabel.setStyle("-fx-text-fill: rgb(34, 197, 94); -fx-font-weight: bold;");}
    }
    @FXML void handleAddTransactionAction(ActionEvent event) { System.out.println("Tombol Tambah Transaksi diklik."); openTransactionDialog(null, false); }


    private void handleEditTransactionAction(Transaction transaction) {
        System.out.println("Edit transaksi: " + transaction.getDescription());
        openTransactionDialog(transaction, false); // false menandakan bukan view mode (ini mode edit)
    }

    private void handleViewDetailAction(Transaction transaction) { // Method baru
        System.out.println("Lihat detail transaksi: " + transaction.getDescription());
        openTransactionDialog(transaction, true); // true menandakan view mode
    }

    // ... (handleDeleteTransactionAction, handleApplyFilterAction, handleResetFilterAction sudah baik) ...
    private void handleDeleteTransactionAction(Transaction transaction) {
        System.out.println("Mencoba menghapus transaksi: " + transaction.getDescription());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus Transaksi");
        alert.setHeaderText("Hapus Transaksi: " + transaction.getDescription());
        alert.setContentText("Anda yakin ingin menghapus transaksi ini?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (transactionService.deleteTransaction(transaction.getTransactionId())) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Transaksi berhasil dihapus.");
                loadTransactions();
            } else {
                showAlert(Alert.AlertType.ERROR, "Gagal Hapus", "Gagal menghapus transaksi.");
            }
        }
    }
    @FXML void handleApplyFilterAction(ActionEvent event) { System.out.println("Filter diterapkan."); loadTransactions(); }
    @FXML void handleResetFilterAction(ActionEvent event) { System.out.println("Filter direset."); startDatePicker.setValue(LocalDate.now().withDayOfMonth(1)); endDatePicker.setValue(LocalDate.now()); filterCategoryComboBox.setValue(null); searchKeywordField.clear(); loadTransactions(); }


    // Modifikasi openTransactionDialog untuk menerima mode view/edit
    private void openTransactionDialog(Transaction transaction, boolean isViewOnly) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/neracaku/neracaku/fxml/AddEditTransactionDialog.fxml"));
            Parent root = loader.load();

            AddEditTransactionDialogController controller = loader.getController();

            if (isViewOnly) {
                controller.setTransactionToView(transaction);
            } else if (transaction != null) { // Mode Edit
                controller.setTransactionToEdit(transaction);
            } else { // Mode Tambah Baru
                controller.setNewTransactionMode();
            }

            controller.setDialogHandler(this::loadTransactions); // Tetap set handler

            Stage dialogStage = new Stage();
            dialogStage.setTitle(isViewOnly ? "Detail Transaksi" : (transaction == null ? "Tambah Transaksi Baru" : "Edit Transaksi"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            Stage ownerStage = (Stage) addTransactionButton.getScene().getWindow(); // Ambil stage dari tombol tambah
            if (ownerStage == null && transactionTableView.getScene() != null) { // Fallback jika addTransactionButton tidak ada (jarang terjadi)
                ownerStage = (Stage) transactionTableView.getScene().getWindow();
            }
            dialogStage.initOwner(ownerStage);


            Scene scene = new Scene(root);
            // Pastikan stylesheet dialog dimuat
            URL cssUrl = getClass().getResource("/org/neracaku/neracaku/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Gagal memuat styles.css untuk dialog transaksi.");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error Dialog", "Gagal membuka dialog transaksi: " + e.getMessage());
        }
    }

    // ... (showAlert dan capitalize tetap sama) ...
    private void showAlert(Alert.AlertType alertType, String title, String message) { Alert alert = new Alert(alertType); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait(); }
    private String capitalize(String str) { if (str == null || str.isEmpty()) return str; return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase(); }
}