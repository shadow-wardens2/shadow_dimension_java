import java.sql.*;

public class ListTables {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/shadow_dimensions";
        try (Connection conn = DriverManager.getConnection(url, "root", "")) {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(null, null, "%", null);
            while (rs.next()) {
                System.out.println(rs.getString(3));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
