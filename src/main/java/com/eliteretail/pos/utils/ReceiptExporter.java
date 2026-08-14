package com.eliteretail.pos.utils;

import com.eliteretail.pos.model.ProductItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReceiptExporter {
    private static final Logger LOGGER = Logger.getLogger(ReceiptExporter.class.getName());
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float LEFT_MARGIN = 70;
    private static final float TOP_Y = 800;
    private static final float BOTTOM_Y = 100;

    private ReceiptExporter() {}

    public static Path exportReceiptToPDF(
            String billNumber,
            String cashierName,
            List<ProductItem> purchasedItems,
            double subtotal,
            double discount,
            double total,
            String paymentMethod,
            double received,
            double change
    ) throws IOException {
        Path receiptDirectory = Path.of(System.getProperty("user.dir"), "receipts");
        Files.createDirectories(receiptDirectory);
        Path output = receiptDirectory.resolve("receipt_" + sanitizeFileName(billNumber) + ".pdf");

        try (PDDocument document = new PDDocument()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PageWriter writer = createPage(document, regular, bold, billNumber, cashierName, true);
            try {
                for (ProductItem item : purchasedItems) {
                    if (writer.y < BOTTOM_Y + 40) {
                        writer.close();
                        writer = createPage(document, regular, bold, billNumber, cashierName, false);
                    }
                    writer.y = writeItem(writer.content, regular, writer.y, item);
                }

                if (writer.y < BOTTOM_Y + 170) {
                    writer.close();
                    writer = createPage(document, regular, bold, billNumber, cashierName, false);
                }

                drawRule(writer.content, writer.y + 4);
                writer.y -= 18;
                writer.y = writeLine(writer.content, regular, 10, LEFT_MARGIN, writer.y, moneyLine("Subtotal", subtotal), 16);
                writer.y = writeLine(writer.content, regular, 10, LEFT_MARGIN, writer.y, moneyLine("Discount", discount), 16);
                writer.y = writeLine(writer.content, bold, 12, LEFT_MARGIN, writer.y, moneyLine("Total", total), 22);
                writer.y = writeLine(writer.content, regular, 10, LEFT_MARGIN, writer.y, "Payment method: " + paymentMethod, 16);
                writer.y = writeLine(writer.content, regular, 10, LEFT_MARGIN, writer.y, moneyLine("Received", received), 16);
                writer.y = writeLine(writer.content, regular, 10, LEFT_MARGIN, writer.y, moneyLine("Change", change), 24);
                writeLine(writer.content, bold, 10, LEFT_MARGIN, writer.y,
                        "Thank you for shopping with EliteRetail!", 16);
            } finally {
                writer.close();
            }
            document.save(output.toFile());
        }

        openIfSupported(output);
        return output;
    }

    private static PageWriter createPage(
            PDDocument document,
            PDType1Font regular,
            PDType1Font bold,
            String billNumber,
            String cashierName,
            boolean firstPage
    ) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream content = new PDPageContentStream(document, page);

        float y = TOP_Y;
        if (firstPage) {
            drawBrandMark(content, LEFT_MARGIN, TOP_Y - 42);
            writeLine(content, bold, 18, LEFT_MARGIN + 58, TOP_Y - 3, "EliteRetail POS", 20);
            writeLine(content, regular, 9, LEFT_MARGIN + 58, TOP_Y - 24, "RETAIL OPERATIONS PLATFORM", 14);
            y = TOP_Y - 66;
            drawRule(content, y + 10);
            y = writeLine(content, bold, 10, LEFT_MARGIN, y, "Professional Retail Receipt", 16);
            y = writeLine(content, regular, 9, LEFT_MARGIN, y, "Demo Store | Local POS", 16);
            y = writeLine(content, regular, 10, LEFT_MARGIN, y, "Receipt: " + billNumber, 16);
            y = writeLine(content, regular, 10, LEFT_MARGIN, y, "Cashier: " + cashierName, 16);
            y = writeLine(content, regular, 10, LEFT_MARGIN, y,
                    "Date: " + LocalDateTime.now().format(DATE_TIME), 24);
        } else {
            drawBrandMark(content, LEFT_MARGIN, TOP_Y - 36);
            writeLine(content, bold, 14, LEFT_MARGIN + 52, TOP_Y - 4, "EliteRetail POS - Receipt continued", 20);
            y = TOP_Y - 54;
            y = writeLine(content, regular, 9, LEFT_MARGIN, y, "Receipt: " + billNumber, 20);
        }

        content.setNonStrokingColor(11, 31, 58);
        content.addRect(LEFT_MARGIN - 4, y - 4, 470, 20);
        content.fill();
        content.setNonStrokingColor(255, 255, 255);
        y = writeLine(content, bold, 9, LEFT_MARGIN + 4, y + 9,
                String.format(Locale.US, "%-20s %4s %9s %9s %10s",
                        "Item", "Qty", "Price", "Disc", "Total"), 22);
        content.setNonStrokingColor(19, 34, 56);
        return new PageWriter(content, y);
    }

    private static float writeItem(PDPageContentStream content, PDType1Font regular, float y, ProductItem item)
            throws IOException {
        String row = String.format(Locale.US, "%-20s %4d %9.2f %9.2f %10.2f",
                abbreviate(item.getName(), 20),
                item.getStockQuantity(),
                item.getUnitPrice(),
                item.getDiscount(),
                item.getTotal());
        return writeLine(content, regular, 9, LEFT_MARGIN, y, row, 14);
    }

    private static float writeLine(PDPageContentStream content, PDType1Font font, float fontSize,
                                   float x, float y, String text, float lineHeight) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(toPrintableAscii(text));
        content.endText();
        return y - lineHeight;
    }

    private static void drawRule(PDPageContentStream content, float y) throws IOException {
        content.setStrokingColor(217, 226, 238);
        content.setLineWidth(1.0f);
        content.moveTo(LEFT_MARGIN, y);
        content.lineTo(LEFT_MARGIN + 462, y);
        content.stroke();
        content.setNonStrokingColor(19, 34, 56);
    }

    private static void drawBrandMark(PDPageContentStream content, float x, float y) throws IOException {
        // Compact vector version of the EliteRetail analytics/store mark.
        content.setNonStrokingColor(11, 31, 58);
        content.addRect(x, y, 44, 44);
        content.fill();

        content.setNonStrokingColor(127, 180, 255);
        content.addRect(x + 9, y + 9, 6, 14);
        content.fill();
        content.setNonStrokingColor(60, 141, 243);
        content.addRect(x + 19, y + 9, 6, 20);
        content.fill();
        content.setNonStrokingColor(255, 255, 255);
        content.addRect(x + 29, y + 9, 6, 27);
        content.fill();

        content.setStrokingColor(217, 164, 65);
        content.setLineWidth(2.6f);
        content.moveTo(x + 8, y + 31);
        content.lineTo(x + 22, y + 39);
        content.lineTo(x + 36, y + 31);
        content.stroke();
        content.moveTo(x + 8, y + 6);
        content.lineTo(x + 36, y + 6);
        content.stroke();
        content.setNonStrokingColor(19, 34, 56);
    }

    private static String moneyLine(String label, double amount) {
        return String.format(Locale.US, "%-20s $%10.2f", label + ":", amount);
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private static String sanitizeFileName(String value) {
        return value == null ? "receipt" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String toPrintableAscii(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static void openIfSupported(Path file) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException | UnsupportedOperationException e) {
            LOGGER.log(Level.FINE, "Receipt created but could not be opened automatically.", e);
        }
    }

    private static final class PageWriter implements AutoCloseable {
        private final PDPageContentStream content;
        private float y;
        private boolean closed;

        private PageWriter(PDPageContentStream content, float y) {
            this.content = content;
            this.y = y;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                content.close();
                closed = true;
            }
        }
    }
}
