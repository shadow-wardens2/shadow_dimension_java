package Services.Tutorials;

import Entities.Tutorials.Jeu;
import Interfaces.InterfaceServiceTuto;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceJeu implements InterfaceServiceTuto<Jeu> {
    private final Connection connection;

    public ServiceJeu() {
        this.connection = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Jeu jeu) throws SQLException {
        String sql = "INSERT INTO jeu (nom, genre) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, jeu.getNom());
            preparedStatement.setString(2, jeu.getGenre());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    jeu.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Jeu jeu) throws SQLException {
        String sql = "UPDATE jeu SET nom = ?, genre = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, jeu.getNom());
            preparedStatement.setString(2, jeu.getGenre());
            preparedStatement.setInt(3, jeu.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void delete(Jeu jeu) throws SQLException {
        String sql = "DELETE FROM jeu WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, jeu.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<Jeu> getAll() throws SQLException {
        List<Jeu> list = new ArrayList<>();
        String sql = "SELECT * FROM jeu";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Jeu(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("genre")));
            }
        }
        return list;
    }
}
