package Services.Tutorials;

import Entities.Tutorials.Question;
import Entities.Tutorials.Quiz;
import Interfaces.InterfaceServiceTuto;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceQuestion implements InterfaceServiceTuto<Question> {
    private final Connection connection;

    public ServiceQuestion() {
        this.connection = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Question question) throws SQLException {
        String sql = "INSERT INTO question (texte, quiz_id) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, question.getTexte());
            preparedStatement.setObject(2, question.getQuiz() != null ? question.getQuiz().getId() : null);
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    question.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Question question) throws SQLException {
        String sql = "UPDATE question SET texte = ?, quiz_id = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, question.getTexte());
            preparedStatement.setObject(2, question.getQuiz() != null ? question.getQuiz().getId() : null);
            preparedStatement.setInt(3, question.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void delete(Question question) throws SQLException {
        String sql = "DELETE FROM question WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, question.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<Question> getAll() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Question question = new Question();
                question.setId(rs.getInt("id"));
                question.setTexte(rs.getString("texte"));

                int quizId = rs.getInt("quiz_id");
                if (!rs.wasNull()) {
                    Quiz quiz = new Quiz();
                    quiz.setId(quizId);
                    question.setQuiz(quiz);
                }

                list.add(question);
            }
        }
        return list;
    }
}
