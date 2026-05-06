package Utils;

import java.sql.Connection;
import java.sql.Statement;

public class RenameTableScratch {
    public static void main(String[] args) {
        try {
            Connection cnx = ShadowDimensionsDB.getInstance().getConnection();
            if (cnx == null) {
                System.err.println("Failed to connect to database.");
                return;
            }
            Statement st = cnx.createStatement();
            st.execute("RENAME TABLE artworks TO artwork");
            System.out.println("Table renamed successfully from 'artworks' to 'artwork'.");
        } catch (Exception e) {
            System.err.println("Error renaming table: " + e.getMessage());
            // Try ALTER TABLE if RENAME TABLE fails
            try {
                Connection cnx = ShadowDimensionsDB.getInstance().getConnection();
                Statement st = cnx.createStatement();
                st.execute("ALTER TABLE artworks RENAME TO artwork");
                System.out.println("Table renamed successfully using ALTER TABLE.");
            } catch (Exception e2) {
                System.err.println("Fatal error: " + e2.getMessage());
            }
        }
    }
}
