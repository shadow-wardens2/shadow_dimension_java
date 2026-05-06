import Utils.ShadowDimensionsDB;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class GetSchema {
    public static void main(String[] args) {
        try {
            Connection conn = ShadowDimensionsDB.getInstance().getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            System.out.println("Columns in forum_post_reaction:");
            ResultSet rsReaction = metaData.getColumns(null, null, "forum_post_reaction", null);
            while (rsReaction.next()) {
                System.out.println(rsReaction.getString("COLUMN_NAME") + " - " + rsReaction.getString("TYPE_NAME"));
            }

            System.out.println("\nColumns in forum_comment:");
            ResultSet rsComment = metaData.getColumns(null, null, "forum_comment", null);
            while (rsComment.next()) {
                System.out.println(rsComment.getString("COLUMN_NAME") + " - " + rsComment.getString("TYPE_NAME"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
