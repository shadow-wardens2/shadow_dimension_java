package Services;

import Entities.Categorie;
import Interfaces.InterfaceServiceProduit;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCategorie implements InterfaceServiceProduit<Categorie> {

    private Connection cnx;

    public ServiceCategorie() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Categorie c) throws SQLException {
        String req = "INSERT INTO mkt_categorie(nom, description) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.executeUpdate();
    }

    @Override
    public void update(Categorie c) throws SQLException {
        String req = "UPDATE mkt_categorie SET nom=?, description=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setInt(3, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Categorie c) throws SQLException {
        String req = "DELETE FROM mkt_categorie WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, c.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Categorie> getAll() throws SQLException {
        List<Categorie> list = new ArrayList<>();
        String req = "SELECT * FROM mkt_categorie";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(new Categorie(rs.getInt("id"), rs.getString("nom"), rs.getString("description")));
        }
        return list;
    }
}