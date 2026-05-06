package Utils;

import Entities.event.Category;
import Entities.event.Event;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PdfExportUtil {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(35, 35, 35));
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(95, 95, 95));
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
    private static final Font STATUS_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

    private static final Color PURPLE = new Color(126, 84, 255);
    private static final Color TABLE_HEADER = new Color(240, 240, 240);
    private static final Color ROW_EVEN = new Color(250, 250, 250);
    private static final Color ROW_ODD = Color.WHITE;

    private PdfExportUtil() {
    }

    public static void exportEvents(String filePath, List<Event> events) throws IOException, DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        addReportHeader(document, "Events Report");

        PdfPTable table = new PdfPTable(new float[]{2.4f, 1.4f, 1.8f, 1.2f, 1.0f, 1.1f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(16f);

        addHeaderCell(table, "Title");
        addHeaderCell(table, "Category");
        addHeaderCell(table, "Date");
        addHeaderCell(table, "Venue");
        addHeaderCell(table, "Price");
        addHeaderCell(table, "Capacity");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        int rowIndex = 0;
        for (Event event : events) {
            addBodyCell(table, safe(event.getTitle()), rowIndex);
            addBodyCell(table, safe(event.getCategoryName()), rowIndex);

            Timestamp startDate = event.getStartDate();
            addBodyCell(table, startDate == null ? "N/A" : formatter.format(startDate), rowIndex);

            addBodyCell(table, safe(event.getLocation()), rowIndex);
            addBodyCell(table, "N/A", rowIndex);
            addBodyCell(table, String.valueOf(event.getCapacity()), rowIndex);
            rowIndex++;
        }

        document.add(table);
        document.close();
    }

    public static void exportCategories(String filePath, List<Category> categories) throws IOException, DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        addReportHeader(document, "Categories Report");

        PdfPTable table = new PdfPTable(new float[]{1.0f, 2.0f, 3.0f, 1.4f, 1.2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(16f);

        addHeaderCell(table, "ID");
        addHeaderCell(table, "Name");
        addHeaderCell(table, "Description");
        addHeaderCell(table, "Pricing");
        addHeaderCell(table, "Price");

        int rowIndex = 0;
        for (Category category : categories) {
            addBodyCell(table, String.valueOf(category.getId()), rowIndex);
            addBodyCell(table, safe(category.getNom()), rowIndex);
            addBodyCell(table, safe(category.getDescription()), rowIndex);
            addBodyCell(table, safe(category.getTypeTarification()), rowIndex);
            addBodyCell(table, category.getPrix() == null ? "N/A" : String.valueOf(category.getPrix()), rowIndex);
            rowIndex++;
        }

        document.add(table);
        document.close();
    }

    public static void exportReservations(String filePath, List<Entities.event.Reservation> reservations)
            throws IOException, DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        addReportHeader(document, "Reservations Report");

        PdfPTable table = new PdfPTable(new float[]{1.0f, 2.2f, 2.5f, 1.3f, 2.0f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(16f);

        addHeaderCell(table, "ID");
        addHeaderCell(table, "User");
        addHeaderCell(table, "Event");
        addHeaderCell(table, "Status");
        addHeaderCell(table, "Reserved At");

        int rowIndex = 0;
        for (Entities.event.Reservation reservation : reservations) {
            addBodyCell(table, String.valueOf(reservation.getId()), rowIndex);
            addBodyCell(table, safe(reservation.getUsername()), rowIndex);
            addBodyCell(table, safe(reservation.getEventTitle()), rowIndex);
            addStatusCell(table, safe(reservation.getStatusLabel()), rowIndex);
            addBodyCell(table, String.valueOf(reservation.getReservedAt()), rowIndex);
            rowIndex++;
        }

        document.add(table);
        document.close();
    }

    private static void addReportHeader(Document document, String title) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{1.2f, 4.8f});
        header.setWidthPercentage(100);
        header.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        Image logo = loadLogo();
        if (logo != null) {
            logo.scaleToFit(80f, 80f);
            logoCell.addElement(logo);
        }
        header.addCell(logoCell);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph titleP = new Paragraph(title, TITLE_FONT);
        Paragraph generatedOn = new Paragraph("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), SUBTITLE_FONT);
        generatedOn.setSpacingBefore(4f);
        textCell.addElement(titleP);
        textCell.addElement(generatedOn);
        header.addCell(textCell);

        document.add(header);

        Chunk lineBreak = new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(1.6f, 100f, PURPLE, Element.ALIGN_CENTER, -2));
        document.add(lineBreak);
    }

    private static void addHeaderCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, HEADER_FONT));
        cell.setBackgroundColor(TABLE_HEADER);
        cell.setPadding(9f);
        cell.setBorderColor(new Color(190, 190, 190));
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String value, int rowIndex) {
        PdfPCell cell = new PdfPCell(new Phrase(value, CELL_FONT));
        cell.setBackgroundColor(rowIndex % 2 == 0 ? ROW_EVEN : ROW_ODD);
        cell.setPadding(8f);
        cell.setBorderColor(new Color(200, 200, 200));
        cell.setFixedHeight(30f);
        table.addCell(cell);
    }

    private static void addStatusCell(PdfPTable table, String status, int rowIndex) {
        Color statusColor = switch (status.toUpperCase(Locale.ROOT)) {
            case "ACCEPTED" -> new Color(34, 197, 94);
            case "PENDING" -> new Color(245, 158, 11);
            case "DENIED" -> new Color(239, 68, 68);
            default -> new Color(100, 116, 139);
        };

        PdfPCell cell = new PdfPCell(new Phrase(status, STATUS_FONT));
        cell.setBackgroundColor(statusColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBorderColor(new Color(200, 200, 200));
        cell.setFixedHeight(28f);
        table.addCell(cell);
    }

    private static Image loadLogo() {
        try {
            Path logoPath = Paths.get(System.getProperty("user.dir"), "assets", "images", "logo", "shadow-logo.png");
            if (!Files.exists(logoPath)) {
                return null;
            }
            return Image.getInstance(logoPath.toAbsolutePath().toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
