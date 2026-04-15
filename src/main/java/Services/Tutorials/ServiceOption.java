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
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, option.getTexte());
            ps.setBoolean(2, option.isEstCorrecte());
            ps.setObject(3, option.getQuestion() != null ? option.getQuestion().getId() : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    option.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Option option) throws SQLException {
        String sql = "UPDATE `option` SET texte = ?, est_correcte = ?, question_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, option.getTexte());
            ps.setBoolean(2, option.isEstCorrecte());
            ps.setObject(3, option.getQuestion() != null ? option.getQuestion().getId() : null);
            ps.setInt(4, option.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Option option) throws SQLException {
        String sql = "DELETE FROM `option` WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, option.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Option> getAll() throws SQLException {
        List<Option> list = new ArrayList<>();
        String sql = "SELECT * FROM `option`";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Option option = new Option();
                option.setId(rs.getInt("id"));
                option.setTexte(rs.getString("texte"));
                option.setEstCorrecte(rs.getBoolean("est_correcte"));

                int questionId = rs.getInt("question_id");
                if (!rs.wasNull()) {
                    Question q = new Question();
                    q.setId(questionId);
                    option.setQuestion(q);
                }
                list.add(option);
            }
        }
        return list;
    }
}
