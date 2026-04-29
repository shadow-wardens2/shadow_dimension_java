package Services.Marketplace;

import Entities.Marketplace.Produit;
import Interfaces.InterfaceServiceProduit;
import Entities.User.User;
import Services.User.ServiceUser;
import Utils.ShadowDimensionsDB; // your DB connection class

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceProduit implements InterfaceServiceProduit<Produit> {

    private final ShadowDimensionsDB MyConnection;
    private Connection cnx;

    public ServiceProduit() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
        MyConnection = ShadowDimensionsDB.getInstance();
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
        
        // Trigger low stock alert if needed
        checkStockAndNotify(p);
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

    public Produit getById(int id) throws SQLException {
        String req = "SELECT * FROM mkt_produit WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Produit(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getDouble("prix"),
                    rs.getInt("stock"),
                    rs.getInt("categorie_id"),
                    rs.getInt("type_id"),
                    rs.getString("image"));
        }
        return null;
    }

    private void checkStockAndNotify(Produit p) {
        if (p.getStock() < 5) {
            new Thread(() -> {
                try {
                    ServiceUser su = new ServiceUser();
                    User admin = su.getFirstActiveAdmin();
                    if (admin != null) {
                        String subject = "Low Stock Alert: " + p.getNom();
                        String body = "Hello " + admin.getUsername() + ",\n\n" +
                                      "The product '" + p.getNom() + "' (ID: " + p.getId() + ") is running low on stock.\n" +
                                      "Current stock: " + p.getStock() + "\n\n" +
                                      "Please restock soon.\n\n" +
                                      "Regards,\n" +
                                      "Shadow Dimensions Inventory System";
                        
                        MailService.sendMail(admin.getEmail(), subject, body);
                    }
                } catch (SQLException e) {
                    System.err.println("Failed to check admin for stock alert: " + e.getMessage());
                }
            }).start();
        }
    }
}