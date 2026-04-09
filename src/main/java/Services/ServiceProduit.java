package Services;

import Entities.Produit;
import Interfaces.InterfaceServiceProduit;
import Utils.ShadowDimensionsDB; // your DB connection class

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceProduit implements InterfaceServiceProduit<Produit> {

    private final ShadowDimensionsDB MyConnection;
    private Connection cnx;

    public ServiceProduit() {
        ShadowDimensionsDB myConnection = null;
        MyConnection = myConnection;
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Produit p) throws SQLException {
        String req = "INSERT INTO mkt_produit(nom, description, prix, stock, categorie_id, type_id, image) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, p.getNom());
        ps.setString(2, p.getDescription());
        ps.setDouble(3, p.getPrix());
        ps.setInt(4, p.getStock());
        ps.setInt(5, p.getCategorieId());
        ps.setInt(6, p.getTypeId());
        ps.setString(7, p.getImage());
        ps.executeUpdate();
    }

    @Override
    public void update(Produit p) throws SQLException {
        String req = "UPDATE mkt_produit SET nom=?, description=?, prix=?, stock=?, categorie_id=?, type_id=?, image=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, p.getNom());
        ps.setString(2, p.getDescription());
        ps.setDouble(3, p.getPrix());
        ps.setInt(4, p.getStock());
        ps.setInt(5, p.getCategorieId());
        ps.setInt(6, p.getTypeId());
        ps.setString(7, p.getImage());
        ps.setInt(8, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Produit p) throws SQLException {
        String req = "DELETE FROM mkt_produit WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, p.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Produit> getAll() throws SQLException {
        List<Produit> list = new ArrayList<>();
        String req = "SELECT * FROM mkt_produit";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(new Produit(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getDouble("prix"),
                    rs.getInt("stock"),
                    rs.getInt("categorie_id"),
                    rs.getInt("type_id"),
                    rs.getString("image")));
        }
        return list;
    }
}