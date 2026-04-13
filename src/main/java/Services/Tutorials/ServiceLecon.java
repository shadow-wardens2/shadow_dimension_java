package Services.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Lecon;
import Interfaces.InterfaceServiceTuto;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceLecon implements InterfaceServiceTuto<Lecon> {
    private final Connection connection;

    public ServiceLecon() {
        this.connection = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Lecon lecon) throws SQLException {
        String sql = "INSERT INTO lecon (titre, contenu, ordre, formation_id, image, video_url, document_url, video_duration, video_thumbnail) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, lecon.getTitre());
            preparedStatement.setString(2, lecon.getContenu());
            preparedStatement.setInt(3, lecon.getOrdre());
            preparedStatement.setObject(4, lecon.getFormation() != null ? lecon.getFormation().getId() : null);
            preparedStatement.setString(5, lecon.getImage());
            preparedStatement.setString(6, lecon.getVideoUrl());
            preparedStatement.setString(7, lecon.getDocumentUrl());
            preparedStatement.setString(8, lecon.getVideoDuration());
            preparedStatement.setString(9, lecon.getVideoThumbnail());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    lecon.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Lecon lecon) throws SQLException {
        String sql = "UPDATE lecon SET titre = ?, contenu = ?, ordre = ?, formation_id = ?, image = ?, video_url = ?, document_url = ?, video_duration = ?, video_thumbnail = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, lecon.getTitre());
            preparedStatement.setString(2, lecon.getContenu());
            preparedStatement.setInt(3, lecon.getOrdre());
            preparedStatement.setObject(4, lecon.getFormation() != null ? lecon.getFormation().getId() : null);
            preparedStatement.setString(5, lecon.getImage());
            preparedStatement.setString(6, lecon.getVideoUrl());
            preparedStatement.setString(7, lecon.getDocumentUrl());
            preparedStatement.setString(8, lecon.getVideoDuration());
            preparedStatement.setString(9, lecon.getVideoThumbnail());
            preparedStatement.setInt(10, lecon.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void delete(Lecon lecon) throws SQLException {
        String sql = "DELETE FROM lecon WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, lecon.getId());
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<Lecon> getAll() throws SQLException {
        List<Lecon> list = new ArrayList<>();
        String sql = "SELECT * FROM lecon";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Lecon lecon = new Lecon();
                lecon.setId(rs.getInt("id"));
                lecon.setTitre(rs.getString("titre"));
                lecon.setContenu(rs.getString("contenu"));
                lecon.setOrdre(rs.getInt("ordre"));
                lecon.setImage(rs.getString("image"));
                lecon.setVideoUrl(rs.getString("video_url"));
                lecon.setDocumentUrl(rs.getString("document_url"));
                lecon.setVideoDuration(rs.getString("video_duration"));
                lecon.setVideoThumbnail(rs.getString("video_thumbnail"));

                int formationId = rs.getInt("formation_id");
                if (!rs.wasNull()) {
                    Formation formation = new Formation();
                    formation.setId(formationId);
                    lecon.setFormation(formation);
                }

                list.add(lecon);
            }
        }
        return list;
    }
}
