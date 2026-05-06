package Services.Artworks;

import Entities.Artworks.Evaluation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceEvaluationsTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private ServiceEvaluations serviceEvaluations;

    @BeforeEach
    void setUp() {
        serviceEvaluations = new ServiceEvaluations(connection);
    }

    @Test
    void testAddEvaluation() throws SQLException {
        Evaluation eval = new Evaluation(0, 1, 1, 5, "Comment", "now");
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceEvaluations.add(eval);

        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).setInt(3, 5);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testGetAverageRating() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getDouble("avg_rating")).thenReturn(4.5);

        double avg = serviceEvaluations.getAverageRating(1);

        assertEquals(4.5, avg);
    }

    @Test
    void testDeleteEvaluation() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceEvaluations.delete(1);

        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).executeUpdate();
    }
}
