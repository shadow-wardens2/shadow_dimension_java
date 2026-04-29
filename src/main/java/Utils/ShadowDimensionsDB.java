package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ShadowDimensionsDB {

    public String Path ="jdbc:mysql://localhost:3306/shadow_dimensions";
    public String User ="root";
    public String Password ="";
    public Connection connection;
    public static ShadowDimensionsDB instance;
    public ShadowDimensionsDB(){
        try {
            connection= DriverManager.getConnection(Path,User,Password);
            System.out.println("ShadowDimensionsDB connection established");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public static ShadowDimensionsDB getInstance(){
        if(instance==null){
            instance=new ShadowDimensionsDB();
        }
        return instance;
    }

    public Connection getConnection(){
        return connection;
    }
}
