package Services.Marketplace;

import Entities.Marketplace.Commande;
import Entities.Marketplace.Produit;
import Entities.User.User;
import Services.User.ServiceUser;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class ServiceCommande {
    private Connection conn;

    public ServiceCommande() {
        conn = ShadowDimensionsDB.getInstance().getConnection();
    }

    public void add(Commande c) throws SQLException {
        String query = "INSERT INTO mkt_commande (date_commande, total_amount, status, nom, prenom, email, adresse, ville, code_postal, telephone, pays, user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pst.setTimestamp(1, c.getDateCommande());
        pst.setDouble(2, c.getTotalAmount());
        pst.setString(3, c.getStatus());
        pst.setString(4, c.getNom());
        pst.setString(5, c.getPrenom());
        pst.setString(6, c.getEmail());
        pst.setString(7, c.getAdresse());
        pst.setString(8, c.getVille());
        pst.setString(9, c.getCodePostal());
        pst.setString(10, c.getTelephone());
        pst.setString(11, c.getPays());
        pst.setInt(12, c.getUserId());
        pst.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
        pst.setTimestamp(14, new Timestamp(System.currentTimeMillis()));

        pst.executeUpdate();

        ResultSet rs = pst.getGeneratedKeys();
        int commandeId = -1;
        if (rs.next()) {
            commandeId = rs.getInt(1);
            c.setId(commandeId);
        }

        if (commandeId != -1 && c.getProduits() != null) {
            String q2 = "INSERT INTO mkt_commande_produit (commande_id, produit_id) VALUES (?, ?)";
            String qStock = "UPDATE mkt_produit SET stock = stock - 1 WHERE id = ? AND stock > 0";
            PreparedStatement pst2 = conn.prepareStatement(q2);
            PreparedStatement pstStock = conn.prepareStatement(qStock);
            
            for (Produit p : c.getProduits()) {
                pst2.setInt(1, commandeId);
                pst2.setInt(2, p.getId());
                pst2.addBatch();
                
                pstStock.setInt(1, p.getId());
                pstStock.addBatch();
            }
            pst2.executeBatch();
            pstStock.executeBatch();

            // Stock Check and Mailing Service
            ServiceProduit sp = new ServiceProduit();
            ServiceUser su = new ServiceUser();
            for (Produit p : c.getProduits()) {
                Produit updatedProduct = sp.getById(p.getId());
                if (updatedProduct != null && updatedProduct.getStock() < 5) {
                    User admin = su.getFirstActiveAdmin();
                    if (admin != null) {
                        String subject = "Low Stock Alert: " + updatedProduct.getNom();
                        String body = "Hello " + admin.getUsername() + ",\n\n" +
                                      "The product '" + updatedProduct.getNom() + "' (ID: " + updatedProduct.getId() + ") is running low on stock.\n" +
                                      "Current stock: " + updatedProduct.getStock() + "\n\n" +
                                      "Please restock soon.\n\n" +
                                      "Regards,\n" +
                                      "Shadow Dimensions Inventory System";
                        
                        // Send mail in a separate thread to avoid blocking the main UI thread
                        new Thread(() -> MailService.sendMail(admin.getEmail(), subject, body)).start();
                    }
                }
            }
        }
    }

    public List<Commande> getAll() throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        String query = "SELECT * FROM mkt_commande ORDER BY date_commande DESC";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) {
            Commande c = new Commande();
            c.setId(rs.getInt("id"));
            c.setDateCommande(rs.getTimestamp("date_commande"));
            c.setTotalAmount(rs.getDouble("total_amount"));
            c.setStatus(rs.getString("status"));
            c.setNom(rs.getString("nom"));
            c.setPrenom(rs.getString("prenom"));
            c.setEmail(rs.getString("email"));
            c.setAdresse(rs.getString("adresse"));
            c.setVille(rs.getString("ville"));
            c.setCodePostal(rs.getString("code_postal"));
            c.setTelephone(rs.getString("telephone"));
            c.setPays(rs.getString("pays"));
            c.setUserId(rs.getInt("user_id"));
            commandes.add(c);
        }
        return commandes;
    }

    public void delete(Commande c) throws SQLException {
        String q1 = "DELETE FROM mkt_commande_produit WHERE commande_id = ?";
        PreparedStatement pst1 = conn.prepareStatement(q1);
        pst1.setInt(1, c.getId());
        pst1.executeUpdate();
        
        String q2 = "DELETE FROM mkt_commande WHERE id = ?";
        PreparedStatement pst2 = conn.prepareStatement(q2);
        pst2.setInt(1, c.getId());
        pst2.executeUpdate();
    }

    public List<Produit> getOrderedProductsByUserId(int userId) throws SQLException {
        List<Produit> list = new ArrayList<>();
        String req = "SELECT p.* FROM mkt_produit p " +
                     "JOIN mkt_commande_produit cp ON p.id = cp.produit_id " +
                     "JOIN mkt_commande c ON cp.commande_id = c.id " +
                     "WHERE c.user_id = ? " +
                     "GROUP BY p.id";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
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
