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
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, quiz.getTitre());
            ps.setInt(2, quiz.getOrdre());
            ps.setObject(3, quiz.getFormation() != null ? quiz.getFormation().getId() : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    quiz.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Quiz quiz) throws SQLException {
        String sql = "UPDATE quiz SET titre = ?, ordre = ?, formation_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, quiz.getTitre());
            ps.setInt(2, quiz.getOrdre());
            ps.setObject(3, quiz.getFormation() != null ? quiz.getFormation().getId() : null);
            ps.setInt(4, quiz.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Quiz quiz) throws SQLException {
        String sql = "DELETE FROM quiz WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, quiz.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Quiz> getAll() throws SQLException {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Quiz quiz = new Quiz();
                quiz.setId(rs.getInt("id"));
                quiz.setTitre(rs.getString("titre"));
                quiz.setOrdre(rs.getInt("ordre"));

                int formationId = rs.getInt("formation_id");
                if (!rs.wasNull()) {
                    Formation f = new Formation();
                    f.setId(formationId);
                    quiz.setFormation(f);
                }
                list.add(quiz);
            }
        }
        return list;
    }
}
