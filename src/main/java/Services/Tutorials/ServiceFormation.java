package Services.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Jeu;
import Interfaces.InterfaceServiceTuto;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceFormation implements InterfaceServiceTuto<Formation> {
    private final Connection connection;

    public ServiceFormation() {
        this.connection = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Formation formation) throws SQLException {
        String sql = "INSERT INTO formation (titre, description, niveau, jeu_id, image) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, formation.getTitre());
            preparedStatement.setString(2, formation.getDescription());
            preparedStatement.setString(3, formation.getNiveau());
            preparedStatement.setObject(4, formation.getJeu() != null ? formation.getJeu().getId() : null);
            preparedStatement.setString(5, formation.getImage());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    formation.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Formation formation) throws SQLException {
        String sql = "UPDATE formation SET titre = ?, description = ?, niveau = ?, jeu_id = ?, image = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, formation.getTitre());
            preparedStatement.setString(2, formation.getDescription());
            preparedStatement.setString(3, formation.getNiveau());
            preparedStatement.setObject(4, formation.getJeu() != null ? formation.getJeu().getId() : null);
            preparedStatement.setString(5, formation.getImage());
            preparedStatement.setInt(6, formation.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void delete(Formation formation) throws SQLException {
        String sql = "DELETE FROM formation WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, formation.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<Formation> getAll() throws SQLException {
        List<Formation> list = new ArrayList<>();
        String sql = "SELECT * FROM formation";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Formation formation = new Formation();
                formation.setId(rs.getInt("id"));
                formation.setTitre(rs.getString("titre"));
                formation.setDescription(rs.getString("description"));
                formation.setNiveau(rs.getString("niveau"));
                formation.setImage(rs.getString("image"));

                int jeuId = rs.getInt("jeu_id");
                if (!rs.wasNull()) {
                    Jeu jeu = new Jeu();
                    jeu.setId(jeuId);
                    formation.setJeu(jeu);
                }

                list.add(formation);
            }
        }
        return list;
    }
}
