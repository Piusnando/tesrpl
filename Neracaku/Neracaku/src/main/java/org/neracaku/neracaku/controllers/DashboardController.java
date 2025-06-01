package org.neracaku.neracaku.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color; // Import Color untuk styling programatik jika perlu
import org.neracaku.neracaku.models.MonthlySummary;
import org.neracaku.neracaku.services.TransactionService;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    @FXML
    private Label saldoLabel;
    @FXML
    private Label saldoDescLabel; // Anda bisa gunakan ini untuk info "12 Bln Terakhir"
    @FXML
    private Label pemasukanBulanIniLabel;
    @FXML
    private Label pemasukanComparisonLabel; // Label untuk perbandingan pemasukan
    @FXML
    private Label pemasukanDescLabel; // Deskripsi di bawah nilai pemasukan
    @FXML
    private Label pengeluaranBulanIniLabel;
    @FXML
    private Label pengeluaranComparisonLabel; // Label untuk perbandingan pengeluaran
    @FXML
    private Label pengeluaranDescLabel; // Deskripsi di bawah nilai pengeluaran
    @FXML
    private PieChart categoryPieChart;
    @FXML
    private LineChart<String, Number> trendLineChart;
    @FXML
    private CategoryAxis trendChartXAxis;
    @FXML
    private NumberAxis trendChartYAxis;

    private TransactionService transactionService;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    // Formatter untuk judul chart dan deskripsi KPI
    private final DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));


    @FXML
    public void initialize() {
        System.out.println("DashboardController initialized.");
        transactionService = new TransactionService();
        loadDashboardData();
    }

    private void loadDashboardData() {
        // 1. Saldo Saat Ini
        double currentBalance = transactionService.getCurrentBalance();
        saldoLabel.setText(currencyFormatter.format(currentBalance));
        if (currentBalance < 0) {
            saldoLabel.getStyleClass().removeAll("kpi-value-positive", "kpi-value-neutral"); // Hapus kelas lain
            saldoLabel.getStyleClass().add("kpi-value-negative"); // Tambah kelas untuk warna merah
        } else if (currentBalance > 0) {
            saldoLabel.getStyleClass().removeAll("kpi-value-negative", "kpi-value-neutral");
            saldoLabel.getStyleClass().add("kpi-value-positive"); // Tambah kelas untuk warna hijau
        } else {
            saldoLabel.getStyleClass().removeAll("kpi-value-positive", "kpi-value-negative");
            saldoLabel.getStyleClass().add("kpi-value-neutral"); // Warna default/netral
        }
        saldoDescLabel.setText("Total saldo 12 bulan terakhir."); // Contoh deskripsi

        // 2. Pemasukan & Pengeluaran Bulan Ini
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        String currentMonthYearFormatted = today.format(monthYearFormatter);

        // Pemasukan Bulan Ini
        double monthlyIncome = transactionService.getTotalPemasukan(startOfMonth, endOfMonth);
        pemasukanBulanIniLabel.setText(currencyFormatter.format(monthlyIncome));
        pemasukanDescLabel.setText("Total pemasukan untuk " + currentMonthYearFormatted);

        // Perbandingan Pemasukan
        double lastMonthIncome = transactionService.getTotalPemasukanBulanLalu();
        updateComparisonLabel(pemasukanComparisonLabel, monthlyIncome, lastMonthIncome, "vs Bulan Lalu", false);

        // Pengeluaran Bulan Ini
        double monthlyExpense = transactionService.getTotalPengeluaran(startOfMonth, endOfMonth);
        pengeluaranBulanIniLabel.setText(currencyFormatter.format(monthlyExpense));
        pengeluaranDescLabel.setText("Total pengeluaran untuk " + currentMonthYearFormatted);

        // Perbandingan Pengeluaran
        double lastMonthExpense = transactionService.getTotalPengeluaranBulanLalu();
        updateComparisonLabel(pengeluaranComparisonLabel, monthlyExpense, lastMonthExpense, "vs Bulan Lalu", true); // true karena kenaikan pengeluaran itu negatif

        // 3. Pie Chart Pengeluaran per Kategori Bulan Ini
        loadCategoryPieChartData(YearMonth.from(today));

        // 4. Line Chart Tren Pemasukan vs Pengeluaran
        loadTrendLineChartData(6); // Ambil data untuk 6 bulan terakhir
    }

    private void updateComparisonLabel(Label label, double currentValue, double previousValue, String periodText, boolean higherIsWorse) {
        // Hapus style class sebelumnya untuk warna
        label.getStyleClass().removeAll("kpi-card-comparison-positive", "kpi-card-comparison-negative", "kpi-card-comparison-neutral");

        if (previousValue == 0) {
            if (currentValue == 0) {
                label.setText("Tidak ada data " + periodText.toLowerCase());
                label.getStyleClass().add("kpi-card-comparison-neutral");
            } else { // Ada nilai sekarang, tapi tidak ada nilai sebelumnya (pertumbuhan tak terhingga / baru)
                String arrow = currentValue > 0 ? "↑" : (currentValue < 0 ? "↓" : "");
                label.setText(String.format("%s Baru %s", arrow, periodText.toLowerCase()));
                if (currentValue > 0) {
                    label.getStyleClass().add(higherIsWorse ? "kpi-card-comparison-negative" : "kpi-card-comparison-positive");
                } else if (currentValue < 0) {
                    label.getStyleClass().add(higherIsWorse ? "kpi-card-comparison-positive" : "kpi-card-comparison-negative");
                } else {
                    label.getStyleClass().add("kpi-card-comparison-neutral");
                }
            }
        } else if (currentValue == previousValue) {
            label.setText("Sama " + periodText.toLowerCase());
            label.getStyleClass().add("kpi-card-comparison-neutral");
        } else {
            double change = currentValue - previousValue;
            double percentageChange = (change / Math.abs(previousValue)) * 100;
            String arrow = (change > 0) ? "↑" : "↓";
            String formattedPercentage = String.format(Locale.US, "%.1f%%", Math.abs(percentageChange));
            label.setText(String.format("%s %s %s", arrow, formattedPercentage, periodText));

            if (change > 0) {
                label.getStyleClass().add(higherIsWorse ? "kpi-card-comparison-negative" : "kpi-card-comparison-positive");
            } else { // change < 0
                label.getStyleClass().add(higherIsWorse ? "kpi-card-comparison-positive" : "kpi-card-comparison-negative");
            }
        }
    }


    private void loadCategoryPieChartData(YearMonth yearMonth) {
        Map<String, Double> expensesByCategory = transactionService.getMonthlyExpensesByCategory(yearMonth);
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        String chartTitleText = "Pengeluaran per Kategori - " + yearMonth.format(monthYearFormatter);

        if (expensesByCategory.isEmpty()) {
            categoryPieChart.setTitle("Tidak ada data pengeluaran untuk " + yearMonth.format(monthYearFormatter));
            // Tampilkan slice "Tidak ada data" agar chart tidak kosong sama sekali
            PieChart.Data noDataSlice = new PieChart.Data("Tidak ada data", 1);
            pieChartData.add(noDataSlice);
            categoryPieChart.setData(pieChartData);
            // Atur warna slice "Tidak ada data" menjadi abu-abu
            Platform.runLater(() -> { // Pastikan node sudah ada
                if (noDataSlice.getNode() != null) {
                    noDataSlice.getNode().setStyle("-fx-pie-color: rgb(100, 100, 100);");
                }
            });
        } else {
            categoryPieChart.setTitle(chartTitleText);
            for (Map.Entry<String, Double> entry : expensesByCategory.entrySet()) {
                if (entry.getValue() > 0) {
                    // Tampilkan nama kategori dan persentase atau jumlah di legenda/tooltip
                    // Untuk label di PieChart.Data, kita bisa buat lebih simpel, detail ada di tooltip atau legenda
                    pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                }
            }
            categoryPieChart.setData(pieChartData);

            // Atur warna slice secara programatik jika diperlukan (contoh)
            String[] sliceColors = {
                    "rgb(129, 140, 248)", /* --chart-1 */
                    "rgb(34, 197, 94)",   /* --chart-2 */
                    "rgb(236, 72, 153)",  /* --chart-3 */
                    "rgb(168, 85, 247)",  /* --chart-4 */
                    "rgb(251, 146, 60)"   /* --chart-5 */
                    // Tambahkan lebih banyak warna jika perlu
            };
            int colorIndex = 0;
            for (PieChart.Data data : pieChartData) {
                if (data.getNode() != null) { // Pastikan node sudah ada
                    data.getNode().setStyle("-fx-pie-color: " + sliceColors[colorIndex % sliceColors.length] + ";");
                }
                // Tambahkan tooltip untuk setiap slice
                Tooltip tooltip = new Tooltip(data.getName() + "\n" + currencyFormatter.format(data.getPieValue()));
                Tooltip.install(data.getNode(), tooltip);
                colorIndex++;
            }
        }

        categoryPieChart.setClockwise(true); // Atau false sesuai preferensi
        categoryPieChart.setLabelLineLength(20);
        categoryPieChart.setLabelsVisible(false); // Sesuai keputusan Anda
        categoryPieChart.setLegendVisible(true); // Pastikan legenda terlihat
    }

    private void loadTrendLineChartData(int numberOfMonths) {
        trendLineChart.getData().clear(); // Bersihkan data lama
        trendLineChart.setTitle("Tren Keuangan " + numberOfMonths + " Bulan Terakhir"); // Bisa juga di FXML

        List<MonthlySummary> monthlyData = transactionService.getRecentMonthlySummaries(numberOfMonths);

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Pemasukan");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Pengeluaran");

        if (monthlyData.isEmpty()) {
            // Handle jika tidak ada data, mungkin tampilkan pesan di chart
            // Untuk LineChart, jika tidak ada data, ia akan kosong.
            // Anda bisa menambahkan node placeholder jika mau.
            trendChartXAxis.setLabel("Bulan (Tidak ada data)");
            trendChartYAxis.setLabel("Jumlah (Rp)");
        } else {
            for (MonthlySummary summary : monthlyData) {
                String monthYearLabel = summary.getFormattedMonthYear(); // Misal "Mei 25"
                incomeSeries.getData().add(new XYChart.Data<>(monthYearLabel, summary.getTotalIncome()));
                expenseSeries.getData().add(new XYChart.Data<>(monthYearLabel, summary.getTotalExpense()));
            }
            trendChartXAxis.setLabel("Bulan"); // Kembalikan label default
        }

        trendLineChart.getData().addAll(incomeSeries, expenseSeries);

        // Styling tambahan untuk LineChart jika perlu (bisa juga di CSS)
        for(XYChart.Series<String, Number> series : trendLineChart.getData()){
            if (series.getName().equals("Pemasukan")) {
                //  -fx-stroke: rgb(34, 197, 94); (hijau)
                for(XYChart.Data<String, Number> data : series.getData()){
                    if (data.getNode() != null) data.getNode().setStyle("-fx-background-color: rgb(34, 197, 94), white; -fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 5px;");
                }
            } else if (series.getName().equals("Pengeluaran")) {
                // -fx-stroke: rgb(248, 113, 113); (merah)
                for(XYChart.Data<String, Number> data : series.getData()){
                    if (data.getNode() != null) data.getNode().setStyle("-fx-background-color: rgb(248, 113, 113), white; -fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 5px;");
                }
            }
        }
    }
}