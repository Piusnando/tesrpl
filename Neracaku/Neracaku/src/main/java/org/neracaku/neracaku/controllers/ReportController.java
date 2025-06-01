package org.neracaku.neracaku.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert; // Pastikan Alert diimport
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.neracaku.neracaku.models.Category;
import org.neracaku.neracaku.models.Transaction;
import org.neracaku.neracaku.services.CategoryService;
import org.neracaku.neracaku.services.TransactionService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
// Optional tidak perlu diimport jika tidak digunakan secara eksplisit di sini

public class ReportController {

    @FXML
    private DatePicker reportStartDatePicker;
    @FXML
    private DatePicker reportEndDatePicker;
    @FXML
    private ComboBox<Category> reportCategoryComboBox;
    @FXML
    private Button showReportButton;
    @FXML
    private Button exportCsvButton;
    @FXML
    private Label reportTitleLabel;
    @FXML
    private TableView<Transaction> reportTableView;
    @FXML
    private TableColumn<Transaction, String> reportDateColumn;
    @FXML
    private TableColumn<Transaction, String> reportCategoryColumn;
    @FXML
    private TableColumn<Transaction, String> reportDescriptionColumn;
    @FXML
    private TableColumn<Transaction, String> reportTypeColumn;
    @FXML
    private TableColumn<Transaction, String> reportAmountColumn;
    @FXML
    private HBox reportSummaryPane;
    @FXML
    private Label reportTotalIncomeLabel;
    @FXML
    private Label reportTotalExpenseLabel;
    @FXML
    private Label reportNetBalanceLabel;

    private TransactionService transactionService;
    private CategoryService categoryService;
    private final ObservableList<Transaction> reportTransactionList = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryFilterList = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    @FXML
    public void initialize() {
        System.out.println("ReportController initialized.");
        transactionService = new TransactionService();
        categoryService = new CategoryService();

        setupReportTableColumns();
        setupReportFilters();

        reportTableView.setItems(reportTransactionList);
        setReportElementsVisible(false); // Sembunyikan elemen laporan awal
        exportCsvButton.setDisable(true);    // Tombol ekspor nonaktif awal
    }

    private void setupReportTableColumns() {
        reportDateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTransactionDate().format(dateFormatter)));
        reportCategoryColumn.setCellValueFactory(cellData -> {
            Category category = categoryService.getCategoryById(cellData.getValue().getCategoryId()).orElse(null);
            return new SimpleStringProperty(category != null ? category.getName() : "N/A");
        });
        reportDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        reportTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(capitalize(cellData.getValue().getType())));
        reportAmountColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getAmount())));
        reportAmountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    private void setupReportFilters() {
        List<Category> allCategories = categoryService.getAllCategoriesForDisplay();
        categoryFilterList.add(null); // Opsi "Semua Kategori"
        categoryFilterList.addAll(allCategories);
        reportCategoryComboBox.setItems(categoryFilterList);
        reportCategoryComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Category category) { return category == null ? "Semua Kategori" : category.getName(); }
            @Override public Category fromString(String string) { return null; /* Tidak dipakai */ }
        });

        reportEndDatePicker.setValue(LocalDate.now());
        reportStartDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
    }

    @FXML
    void handleShowReportAction(ActionEvent event) {
        LocalDate startDate = reportStartDatePicker.getValue();
        LocalDate endDate = reportEndDatePicker.getValue();
        Category selectedCategory = reportCategoryComboBox.getValue();
        Integer categoryId = (selectedCategory != null) ? selectedCategory.getCategoryId() : null;

        if (startDate == null || endDate == null) {
            showAlert(Alert.AlertType.WARNING, "Input Tidak Lengkap", "Tanggal mulai dan tanggal akhir harus diisi.");
            setReportElementsVisible(false);
            exportCsvButton.setDisable(true);
            return;
        }
        if (startDate.isAfter(endDate)) {
            showAlert(Alert.AlertType.WARNING, "Input Tidak Valid", "Tanggal mulai tidak boleh setelah tanggal akhir.");
            setReportElementsVisible(false);
            exportCsvButton.setDisable(true);
            return;
        }

        System.out.println("Menampilkan laporan untuk periode: " + startDate + " s/d " + endDate +
                (categoryId != null ? " Kategori: " + selectedCategory.getName() : " Semua Kategori"));

        List<Transaction> transactions = transactionService.getFilteredTransactionsForCurrentUser(startDate, endDate, categoryId, null);
        reportTransactionList.setAll(transactions);

        String periode = startDate.format(dateFormatter) + " - " + endDate.format(dateFormatter);
        String kat = (selectedCategory != null) ? selectedCategory.getName() : "Semua Kategori";
        reportTitleLabel.setText("Laporan Transaksi Periode: " + periode + " | Kategori: " + kat);

        updateReportSummaryLabels(transactions);

        boolean hasData = !transactions.isEmpty();
        setReportElementsVisible(hasData);
        exportCsvButton.setDisable(!hasData);
    }

    private void updateReportSummaryLabels(List<Transaction> transactions) {
        double income = transactions.stream()
                .filter(t -> "pemasukan".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();
        double expense = transactions.stream()
                .filter(t -> "pengeluaran".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();

        reportTotalIncomeLabel.setText(currencyFormatter.format(income));
        reportTotalExpenseLabel.setText(currencyFormatter.format(expense));
        reportNetBalanceLabel.setText(currencyFormatter.format(income - expense));

        if ((income - expense) < 0) {
            reportNetBalanceLabel.setStyle("-fx-text-fill: rgb(255, 91, 91); -fx-font-weight: bold;");
        } else {
            // Anda bisa menggunakan warna dari palet CSS Anda untuk hijau, misalnya --chart-2
            reportNetBalanceLabel.setStyle("-fx-text-fill: rgb(34, 197, 94); -fx-font-weight: bold;");
        }
    }

    @FXML
    void handleExportCsvAction(ActionEvent event) {
        if (reportTransactionList.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Tidak Ada Data", "Tidak ada data transaksi untuk diekspor.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Laporan CSV");
        String defaultFileName = "Laporan_Transaksi_" +
                (reportStartDatePicker.getValue() != null ? reportStartDatePicker.getValue().toString() : "awal") +
                "_sampai_" +
                (reportEndDatePicker.getValue() != null ? reportEndDatePicker.getValue().toString() : "akhir") +
                ".csv";
        fileChooser.setInitialFileName(defaultFileName.replace(":", "-")); // Ganti karakter tidak valid untuk nama file
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(exportCsvButton.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("Tanggal");
                writer.append(',');
                writer.append("Kategori");
                writer.append(',');
                writer.append("Deskripsi");
                writer.append(',');
                writer.append("Jenis");
                writer.append(',');
                writer.append("Jumlah (Rp)");
                writer.append('\n');

                for (Transaction tx : reportTransactionList) {
                    writer.append(escapeCsv(tx.getTransactionDate().format(dateFormatter)));
                    writer.append(',');
                    Category category = categoryService.getCategoryById(tx.getCategoryId()).orElse(null);
                    writer.append(escapeCsv(category != null ? category.getName() : "N/A"));
                    writer.append(',');
                    writer.append(escapeCsv(tx.getDescription()));
                    writer.append(',');
                    writer.append(escapeCsv(capitalize(tx.getType())));
                    writer.append(',');
                    writer.append(String.valueOf(tx.getAmount())); // Simpan sebagai angka murni
                    writer.append('\n');
                }
                writer.flush();
                showAlert(Alert.AlertType.INFORMATION, "Ekspor Berhasil", "Laporan berhasil diekspor ke:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error Ekspor", "Gagal mengekspor laporan: " + e.getMessage());
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String result = value;
        if (result.contains(",") || result.contains("\"") || result.contains("\n") || result.contains("\r")) {
            result = result.replace("\"", "\"\"");
            result = "\"" + result + "\"";
        }
        return result;
    }

    private void setReportElementsVisible(boolean visible) {
        reportTitleLabel.setVisible(visible);
        reportTitleLabel.setManaged(visible);
        reportTableView.setVisible(visible);
        reportTableView.setManaged(visible);
        reportSummaryPane.setVisible(visible);
        reportSummaryPane.setManaged(visible);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // Implementasi showAlert yang lengkap
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // Tidak menggunakan header
        alert.setContentText(message);
        alert.showAndWait();
    }
}