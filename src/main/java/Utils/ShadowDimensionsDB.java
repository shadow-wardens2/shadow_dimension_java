package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ShadowDimensionsDB {

    public String Path = EnvConfig.get("DB_URL", "jdbc:mysql://localhost:3306/shadow_dimensions");
    public String User = EnvConfig.get("DB_USER", "root");
    public String Password = EnvConfig.get("DB_PASSWORD", "");
    public Connection connection;
    public static ShadowDimensionsDB instance;
    public ShadowDimensionsDB(){
        try {
            connection= DriverManager.getConnection(Path,User,Password);
            System.out.println("ShadowDimensionsDB connection established");
        } catch (SQLException e) {
            System.err.println("ShadowDimensionsDB Connection Failed: " + e.getMessage());
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
