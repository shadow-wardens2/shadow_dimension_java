package Services.Tutorials;

import Entities.Tutorials.Option;
import Entities.Tutorials.Question;
import Interfaces.InterfaceServiceTuto;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceOption implements InterfaceServiceTuto<Option> {
    private final Connection connection;

    public ServiceOption() {
        this.connection = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Option option) throws SQLException {
        String sql = "INSERT INTO `option` (texte, est_correcte, question_id) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, option.getTexte());
            preparedStatement.setBoolean(2, option.isEstCorrecte());
            preparedStatement.setObject(3, option.getQuestion() != null ? option.getQuestion().getId() : null);
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    option.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Option option) throws SQLException {
        String sql = "UPDATE `option` SET texte = ?, est_correcte = ?, question_id = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, option.getTexte());
            preparedStatement.setBoolean(2, option.isEstCorrecte());
            preparedStatement.setObject(3, option.getQuestion() != null ? option.getQuestion().getId() : null);
            preparedStatement.setInt(4, option.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void delete(Option option) throws SQLException {
        String sql = "DELETE FROM `option` WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, option.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<Option> getAll() throws SQLException {
        List<Option> list = new ArrayList<>();
        String sql = "SELECT * FROM `option`";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Option option = new Option();
                option.setId(rs.getInt("id"));
                option.setTexte(rs.getString("texte"));
                option.setEstCorrecte(rs.getBoolean("est_correcte"));

                int questionId = rs.getInt("question_id");
                if (!rs.wasNull()) {
                    Question question = new Question();
                    question.setId(questionId);
                    option.setQuestion(question);
                }

                list.add(option);
            }
        }
        return list;
    }
}
