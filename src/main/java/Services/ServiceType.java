package Services;

import Entities.Type;
import Interfaces.InterfaceServiceProduit;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceType implements InterfaceServiceProduit<Type> {

    private Connection cnx;

    public ServiceType() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Type t) throws SQLException {
        String req = "INSERT INTO mkt_type(nom) VALUES (?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, t.getNom());
        ps.executeUpdate();
    }

    @Override
    public void update(Type t) throws SQLException {
        String req = "UPDATE mkt_type SET nom=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, t.getNom());
        ps.setInt(2, t.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Type t) throws SQLException {
        String req = "DELETE FROM mkt_type WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, t.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Type> getAll() throws SQLException {
        List<Type> list = new ArrayList<>();
        String req = "SELECT * FROM mkt_type";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(new Type(rs.getInt("id"), rs.getString("nom")));
        }
        return list;
    }
}
