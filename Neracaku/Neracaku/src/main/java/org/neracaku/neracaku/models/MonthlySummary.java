package org.neracaku.neracaku.models;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MonthlySummary {
    private YearMonth yearMonth;
    private double totalIncome;
    private double totalExpense;

    // Formatter untuk tampilan di Chart Axis
    public static final DateTimeFormatter CHART_AXIS_FORMATTER = DateTimeFormatter.ofPattern("MMM yy", new Locale("id", "ID"));


    public MonthlySummary(YearMonth yearMonth, double totalIncome, double totalExpense) {
        this.yearMonth = yearMonth;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
    }

    // Untuk ditampilkan di sumbu X LineChart
    public String getFormattedMonthYear() {
        return yearMonth.format(CHART_AXIS_FORMATTER);
    }

    @Override
    public String toString() {
        return "MonthlySummary{yearMonth=" + yearMonth + ", totalIncome=" + totalIncome + ", totalExpense=" + totalExpense + '}';
    }
}