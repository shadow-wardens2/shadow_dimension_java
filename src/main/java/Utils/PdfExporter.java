package Utils;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import Entities.Marketplace.Produit;
import Entities.Marketplace.Categorie;
import Entities.Marketplace.Type;
import Services.Marketplace.ServiceProduit;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceType;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExporter {

    public static void generateMarketplaceReport(File file) throws Exception {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Fonts
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.DARK_GRAY);
            Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("Marketplace Detailed Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), normalFont));
            document.add(new Paragraph(" ")); // Spacer

            // Services
            ServiceProduit sp = new ServiceProduit();
            ServiceCategorie sc = new ServiceCategorie();
            ServiceType st = new ServiceType();

            // Products Section
            document.add(new Paragraph("1. Products", sectionFont));
            document.add(new Paragraph(" "));
            PdfPTable productTable = new PdfPTable(4);
            productTable.setWidthPercentage(100);
            productTable.addCell("ID");
            productTable.addCell("Name");
            productTable.addCell("Price");
            productTable.addCell("Stock");

            List<Produit> products = sp.getAll();
            for (Produit p : products) {
                productTable.addCell(String.valueOf(p.getId()));
                productTable.addCell(p.getNom());
                productTable.addCell(String.valueOf(p.getPrix()));
                productTable.addCell(String.valueOf(p.getStock()));
            }
            document.add(productTable);
            document.add(new Paragraph(" "));

            // Categories Section
            document.add(new Paragraph("2. Categories", sectionFont));
            document.add(new Paragraph(" "));
            PdfPTable categoryTable = new PdfPTable(2);
            categoryTable.setWidthPercentage(100);
            categoryTable.addCell("ID");
            categoryTable.addCell("Name");

            List<Categorie> categories = sc.getAll();
            for (Categorie c : categories) {
                categoryTable.addCell(String.valueOf(c.getId()));
                categoryTable.addCell(c.getNom());
            }
            document.add(categoryTable);
            document.add(new Paragraph(" "));

            // Types Section
            document.add(new Paragraph("3. Types", sectionFont));
            document.add(new Paragraph(" "));
            PdfPTable typeTable = new PdfPTable(2);
            typeTable.setWidthPercentage(100);
            typeTable.addCell("ID");
            typeTable.addCell("Name");

            List<Type> types = st.getAll();
            for (Type t : types) {
                typeTable.addCell(String.valueOf(t.getId()));
                typeTable.addCell(t.getNom());
            }
            document.add(typeTable);

            document.close();
        } catch (Exception e) {
            if (document.isOpen()) document.close();
            throw e;
        }
    }
}
