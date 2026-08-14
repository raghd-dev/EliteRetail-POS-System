package com.eliteretail.pos.utils;

import com.eliteretail.pos.repository.StatisticsRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

public final class ReportExporter {
    private ReportExporter() {}

    public static void exportTodayReportToCsv(Path filePath) throws IOException, SQLException {
        StatisticsRepository statistics = new StatisticsRepository();
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write("Statistic,Value\n");
            writeRow(writer, "Total Sales", statistics.getTotalSalesToday());
            writeRow(writer, "Total Transactions", statistics.getTotalTransactionsToday());
            writeRow(writer, "Average Transaction Value", statistics.getAverageTransactionValue());
            writeRow(writer, "Total Discounts", statistics.getTotalDiscountsToday());

            writer.write("\nPayment Method,Count\n");
            for (Map.Entry<String, Integer> entry : statistics.getPaymentMethodStats().entrySet()) {
                writeRow(writer, entry.getKey(), entry.getValue());
            }

            writer.write("\nTop Product,Sales Amount\n");
            for (Map.Entry<String, Double> entry : statistics.getTopSellingProducts(10).entrySet()) {
                writeRow(writer, entry.getKey(), entry.getValue());
            }
        }
    }

    private static void writeRow(BufferedWriter writer, String label, Object value) throws IOException {
        writer.write(csv(label));
        writer.write(',');
        writer.write(csv(String.valueOf(value)));
        writer.newLine();
    }

    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
