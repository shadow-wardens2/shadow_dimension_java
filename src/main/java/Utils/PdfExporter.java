package Utils;

import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Entities.Marketplace.Type;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceProduit;
import Services.Marketplace.ServiceType;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExporter {

    // --- Ethereal Veil Palette ---
    private static final Color PRIMARY_PURPLE = new Color(164, 118, 255);
    private static final Color DARK_SURFACE = new Color(21, 18, 26);
    private static final Color DARK_LOW = new Color(28, 25, 34);
    private static final Color TEXT_DIM = new Color(173, 170, 174);
    private static final Color SUCCESS_GREEN = new Color(78, 226, 138);
    private static final Color DANGER_RED = new Color(255, 93, 125);

    // --- Fonts ---
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, Color.WHITE);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY_PURPLE);
    private static final Font CARD_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DIM);
    private static final Font CARD_VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font ITALIC_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
    private static final Font STATUS_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

    public static void generateMarketplaceReport(File file) throws Exception {
        Document document = new Document(PageSize.A4, 40, 40, 60, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            writer.setPageEvent(new EtherealPageEvent());

            document.open();

            // 1. STYLIZED HEADER
            addEtherealHeader(document);

            // 2. DASHBOARD SUMMARY
            addDashboardSummary(document);

            // 3. MAIN INVENTORY TABLE
            document.add(new Paragraph(" "));
            addSectionHeader(document, "Shadow Artifacts Inventory");
            addInventoryTable(document);

            // 4. CATEGORIES & METADATA
            document.add(new Paragraph(" "));
            addSectionHeader(document, "Dimensional Classifications");
            addMetadataTables(document);

            // 5. CURATOR'S SIGNATURE
            addCuratorNote(document);

            document.close();
        } catch (Exception e) {
            if (document.isOpen()) document.close();
            throw e;
        }
    }

    private static void addEtherealHeader(Document document) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(DARK_SURFACE);
        cell.setPadding(25);
        cell.setBorder(Rectangle.NO_BORDER);

        // Nested table for Logo and Title
        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        inner.setWidths(new float[]{1, 3});
        inner.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Logo
        Image logo = loadLogo();
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        if (logo != null) {
            logo.scaleToFit(80, 80);
            logoCell.addElement(logo);
        } else {
            logoCell.addElement(new Paragraph("SHADOW", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARY_PURPLE)));
        }
        inner.addCell(logoCell);

        // Title and Subtitle
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        Paragraph title = new Paragraph("MARKETPLACE MASTER LEDGER", TITLE_FONT);
        title.setAlignment(Element.ALIGN_RIGHT);
        
        Paragraph subtitle = new Paragraph("Ethereal Inventory Control System", FontFactory.getFont(FontFactory.HELVETICA, 10, PRIMARY_PURPLE));
        subtitle.setAlignment(Element.ALIGN_RIGHT);
        
        titleCell.addElement(title);
        titleCell.addElement(subtitle);
        inner.addCell(titleCell);

        cell.addElement(inner);
        table.addCell(cell);
        
        document.add(table);
        
        // Date Line
        Paragraph date = new Paragraph("Cycle Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm")), ITALIC_FONT);
        date.setAlignment(Element.ALIGN_RIGHT);
        date.setSpacingBefore(5);
        document.add(date);
    }

    private static void addDashboardSummary(Document document) throws Exception {
        ServiceProduit sp = new ServiceProduit();
        List<Produit> products = sp.getAll();
        
        int totalStock = products.stream().mapToInt(Produit::getStock).sum();
        double avgPrice = products.isEmpty() ? 0 : products.stream().mapToDouble(Produit::getPrix).average().orElse(0);
        long outOfStock = products.stream().filter(p -> p.getStock() <= 0).count();
        double totalValue = products.stream().mapToDouble(p -> p.getPrix() * p.getStock()).sum();

        PdfPTable dashboard = new PdfPTable(4);
        dashboard.setWidthPercentage(100);
        dashboard.setSpacingBefore(15);
        dashboard.setSpacingAfter(15);

        addStatCard(dashboard, "ARTIFACTS", String.valueOf(products.size()));
        addStatCard(dashboard, "TOTAL UNITS", String.valueOf(totalStock));
        addStatCard(dashboard, "AVG ESSENCE (PRICE)", String.format("%.2f $", avgPrice));
        addStatCard(dashboard, "VOIDED (OUT)", String.valueOf(outOfStock));

        document.add(dashboard);
        
        // Total Value Bar
        Paragraph val = new Paragraph("ESTIMATED MARKET VALUATION: " + String.format("%.2f", totalValue) + " $", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, DARK_SURFACE));
        val.setAlignment(Element.ALIGN_CENTER);
        document.add(val);
    }

    private static void addStatCard(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(DARK_LOW);
        cell.setPadding(12);
        cell.setBorderColor(PRIMARY_PURPLE);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        Paragraph l = new Paragraph(label, CARD_TITLE_FONT);
        l.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(l);
        
        Paragraph v = new Paragraph(value, CARD_VALUE_FONT);
        v.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(v);
        
        table.addCell(cell);
    }

    private static void addSectionHeader(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title.toUpperCase(), SECTION_FONT);
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        document.add(p);
        
        LineSeparator line = new LineSeparator(1.5f, 100, PRIMARY_PURPLE, Element.ALIGN_LEFT, -2);
        document.add(new Chunk(line));
        document.add(new Paragraph(" "));
    }

    private static void addInventoryTable(Document document) throws Exception {
        ServiceProduit sp = new ServiceProduit();
        ServiceCategorie sc = new ServiceCategorie();
        List<Produit> products = sp.getAll();

        PdfPTable table = new PdfPTable(new float[]{0.6f, 2.0f, 1.4f, 1.0f, 0.8f, 1.2f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        String[] headers = {"ID", "NAME", "CATEGORY", "PRICE", "STOCK", "STATUS"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            c.setBackgroundColor(PRIMARY_PURPLE);
            c.setPadding(8);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBorderColor(Color.WHITE);
            table.addCell(c);
        }

        List<Categorie> allCategories = sc.getAll();
        int row = 0;
        for (Produit p : products) {
            Color bg = (row % 2 == 0) ? Color.WHITE : new Color(250, 248, 255);
            
            table.addCell(styledCell(String.valueOf(p.getId()), bg, Element.ALIGN_CENTER));
            
            // Name with bold
            PdfPCell nameCell = new PdfPCell(new Phrase(p.getNom(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK)));
            nameCell.setBackgroundColor(bg);
            nameCell.setPadding(6);
            nameCell.setBorderColor(new Color(230, 230, 230));
            table.addCell(nameCell);
            
            Categorie cat = allCategories.stream().filter(c -> c.getId() == p.getCategorieId()).findFirst().orElse(null);
            table.addCell(styledCell(cat != null ? cat.getNom() : "N/A", bg, Element.ALIGN_LEFT));
            table.addCell(styledCell(String.format("%.2f $", p.getPrix()), bg, Element.ALIGN_RIGHT));
            table.addCell(styledCell(String.valueOf(p.getStock()), bg, Element.ALIGN_CENTER));
            
            // Status Badge
            PdfPCell sCell = new PdfPCell();
            sCell.setBackgroundColor(bg);
            sCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            sCell.setPadding(5);
            sCell.setBorderColor(new Color(230, 230, 230));
            
            String status = p.getStock() > 10 ? "INSTOCK" : (p.getStock() > 0 ? "LOW" : "VOID");
            Color sCol = p.getStock() > 10 ? SUCCESS_GREEN : (p.getStock() > 0 ? new Color(245, 158, 11) : DANGER_RED);
            
            PdfPTable badge = new PdfPTable(1);
            PdfPCell bCell = new PdfPCell(new Phrase(status, STATUS_FONT));
            bCell.setBackgroundColor(sCol);
            bCell.setBorder(Rectangle.NO_BORDER);
            bCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            bCell.setPadding(2);
            badge.addCell(bCell);
            sCell.addElement(badge);
            table.addCell(sCell);
            
            row++;
        }

        document.add(table);
    }

    private static void addMetadataTables(Document document) throws Exception {
        PdfPTable container = new PdfPTable(2);
        container.setWidthPercentage(100);
        container.setSpacingBefore(10);
        container.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Categories
        PdfPTable catTable = new PdfPTable(new float[]{1, 3});
        catTable.setWidthPercentage(95);
        addMiniHeader(catTable, "CAT ID", "CATEGORY NAME");
        for (Categorie c : new ServiceCategorie().getAll()) {
            catTable.addCell(styledCell(String.valueOf(c.getId()), Color.WHITE, Element.ALIGN_CENTER));
            catTable.addCell(styledCell(c.getNom(), Color.WHITE, Element.ALIGN_LEFT));
        }
        container.addCell(catTable);

        // Types
        PdfPTable typeTable = new PdfPTable(new float[]{1, 3});
        typeTable.setWidthPercentage(95);
        addMiniHeader(typeTable, "TYPE ID", "VARIANT TYPE");
        for (Type t : new ServiceType().getAll()) {
            typeTable.addCell(styledCell(String.valueOf(t.getId()), Color.WHITE, Element.ALIGN_CENTER));
            typeTable.addCell(styledCell(t.getNom(), Color.WHITE, Element.ALIGN_LEFT));
        }
        container.addCell(typeTable);

        document.add(container);
    }

    private static void addCuratorNote(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);
        
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderWidthLeft(3f);
        cell.setBorderColorLeft(PRIMARY_PURPLE);
        cell.setPadding(15);
        cell.setBackgroundColor(new Color(245, 245, 250));
        
        Paragraph p = new Paragraph("Curator's Note: This ledger reflects the current state of the marketplace within the Shadow Dimensions. Ensure all voided artifacts are restocked before the next cycle.", 
                                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.DARK_GRAY));
        cell.addElement(p);
        
        Paragraph sign = new Paragraph("— The Shadow Curator", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_SURFACE));
        sign.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(sign);
        
        noteTable.addCell(cell);
        document.add(noteTable);
    }

    private static PdfPCell styledCell(String text, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(new Color(230, 230, 230));
        return cell;
    }

    private static void addMiniHeader(PdfPTable table, String h1, String h2) {
        PdfPCell c1 = new PdfPCell(new Phrase(h1, TABLE_HEADER_FONT));
        c1.setBackgroundColor(DARK_SURFACE);
        c1.setPadding(5);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(h2, TABLE_HEADER_FONT));
        c2.setBackgroundColor(DARK_SURFACE);
        c2.setPadding(5);
        table.addCell(c2);
    }

    private static Image loadLogo() {
        try {
            Path[] paths = {
                Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "assets", "images", "logo", "shadow-logo.png"),
                Paths.get(System.getProperty("user.dir"), "assets", "images", "logo", "shadow-logo.png")
            };
            for (Path p : paths) {
                if (Files.exists(p)) return Image.getInstance(p.toAbsolutePath().toString());
            }
        } catch (Exception e) {}
        return null;
    }

    private static class EtherealPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            
            // 1. Watermark
            cb.beginText();
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf, 45);
                cb.setRGBColorFill(245, 240, 255);
                cb.showTextAligned(Element.ALIGN_CENTER, "SHADOW DIMENSIONS", 300, 400, 45);
            } catch (Exception e) {}
            cb.endText();

            // 2. Footer
            cb.beginText();
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf, 8);
                cb.setRGBColorFill(150, 150, 150);
                cb.setTextMatrix(document.left(), 30);
                cb.showText("Marketplace Ledger | Confidential Ethereal Data");
                cb.showTextAligned(Element.ALIGN_RIGHT, "Page " + writer.getPageNumber(), document.right(), 30, 0);
            } catch (Exception e) {}
            cb.endText();
            
            // 3. Border accent
            cb.setLineWidth(1f);
            cb.setRGBColorStroke(164, 118, 255);
            cb.moveTo(document.left(), 25);
            cb.lineTo(document.right(), 25);
            cb.stroke();
        }
    }
}
