package Services.Artworks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceReservationsTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private ServiceReservations serviceReservations;

    @BeforeEach
    void setUp() {
        serviceReservations = new ServiceReservations(connection);
    }

    @Test
    void testExists() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        boolean exists = serviceReservations.exists(1, "test@gmail.com");

        assertTrue(exists);
        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).setString(2, "test@gmail.com");
    }

    @Test
    void testAddReservation() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceReservations.add(1, "test@gmail.com");

        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).setString(2, "test@gmail.com");
        verify(preparedStatement).executeUpdate();
    }
}
