package Services.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Quiz;
import Interfaces.InterfaceServiceTuto;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceQuiz implements InterfaceServiceTuto<Quiz> {
    private final Connection connection;

    public ServiceQuiz() {
        this.connection = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Quiz quiz) throws SQLException {
        String sql = "INSERT INTO quiz (titre, ordre, formation_id) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, quiz.getTitre());
            preparedStatement.setInt(2, quiz.getOrdre());
            preparedStatement.setObject(3, quiz.getFormation() != null ? quiz.getFormation().getId() : null);
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    quiz.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Quiz quiz) throws SQLException {
        String sql = "UPDATE quiz SET titre = ?, ordre = ?, formation_id = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, quiz.getTitre());
            preparedStatement.setInt(2, quiz.getOrdre());
            preparedStatement.setObject(3, quiz.getFormation() != null ? quiz.getFormation().getId() : null);
            preparedStatement.setInt(4, quiz.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void delete(Quiz quiz) throws SQLException {
        String sql = "DELETE FROM quiz WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, quiz.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<Quiz> getAll() throws SQLException {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Quiz quiz = new Quiz();
                quiz.setId(rs.getInt("id"));
                quiz.setTitre(rs.getString("titre"));
                quiz.setOrdre(rs.getInt("ordre"));

                int formationId = rs.getInt("formation_id");
                if (!rs.wasNull()) {
                    Formation formation = new Formation();
                    formation.setId(formationId);
                    quiz.setFormation(formation);
                }

                list.add(quiz);
            }
        }
        return list;
    }
}
