package Utils;

import Entities.event.Category;
import Entities.event.Event;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class PdfExportUtil {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(45, 45, 45));
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(95, 95, 95));
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

    private PdfExportUtil() {
    }

    public static void exportEvents(String filePath, List<Event> events) throws IOException, DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        addReportHeader(document, "Gathering Chronicles Report (Events)");

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

        for (Event event : events) {
            addBodyCell(table, safe(event.getTitle()));
            addBodyCell(table, safe(event.getCategoryName()));

            Timestamp startDate = event.getStartDate();
            addBodyCell(table, startDate == null ? "N/A" : formatter.format(startDate));

            addBodyCell(table, safe(event.getLocation()));
            addBodyCell(table, "N/A");
            addBodyCell(table, String.valueOf(event.getCapacity()));
        }

        document.add(table);
        document.close();
    }

    public static void exportCategories(String filePath, List<Category> categories) throws IOException, DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        addReportHeader(document, "Category Registry Report");

        PdfPTable table = new PdfPTable(new float[]{1.0f, 2.0f, 3.0f, 1.4f, 1.2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(16f);

        addHeaderCell(table, "ID");
        addHeaderCell(table, "Name");
        addHeaderCell(table, "Description");
        addHeaderCell(table, "Pricing");
        addHeaderCell(table, "Price");

        for (Category category : categories) {
            addBodyCell(table, String.valueOf(category.getId()));
            addBodyCell(table, safe(category.getNom()));
            addBodyCell(table, safe(category.getDescription()));
            addBodyCell(table, safe(category.getTypeTarification()));
            addBodyCell(table, category.getPrix() == null ? "N/A" : String.valueOf(category.getPrix()));
        }

        document.add(table);
        document.close();
    }

    private static void addReportHeader(Document document, String title) throws DocumentException {
        Paragraph titleP = new Paragraph(title, TITLE_FONT);
        titleP.setAlignment(Element.ALIGN_CENTER);
        document.add(titleP);

        Paragraph generatedOn = new Paragraph("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), SUBTITLE_FONT);
        generatedOn.setAlignment(Element.ALIGN_CENTER);
        generatedOn.setSpacingBefore(6f);
        generatedOn.setSpacingAfter(8f);
        document.add(generatedOn);

        Chunk lineBreak = new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(1.6f, 100f, Color.DARK_GRAY, Element.ALIGN_CENTER, -2));
        document.add(lineBreak);
    }

    private static void addHeaderCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, HEADER_FONT));
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setPadding(9f);
        cell.setBorderColor(new Color(190, 190, 190));
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, CELL_FONT));
        cell.setPadding(8f);
        cell.setBorderColor(new Color(200, 200, 200));
        cell.setFixedHeight(30f);
        table.addCell(cell);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
