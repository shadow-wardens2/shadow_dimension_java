package Services.Artworks;

import Entities.Artworks.Artworks;
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
class ServiceArtworksTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private ServiceArtworks serviceArtworks;

    @BeforeEach
    void setUp() {
        serviceArtworks = new ServiceArtworks(connection);
    }

    @Test
    void testAddArtwork() throws SQLException {
        Artworks artwork = new Artworks(0, "Title", "Desc", 100, "url", "pdf", "ai", "status", 1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceArtworks.add(artwork);

        verify(preparedStatement).setString(1, "Title");
        verify(preparedStatement).setString(2, "Desc");
        verify(preparedStatement).setInt(3, 100);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testUpdateArtwork() throws SQLException {
        Artworks artwork = new Artworks(1, "Updated Title", "Desc", 100, "url", "pdf", "ai", "status", 1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceArtworks.update(artwork);

        verify(preparedStatement).setString(1, "Updated Title");
        verify(preparedStatement).setInt(9, 1);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testDeleteArtwork() throws SQLException {
        Artworks artwork = new Artworks();
        artwork.setId(1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceArtworks.delete(artwork);

        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testGetAll() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        
        // Mocking metadata for the debug print in ServiceArtworks
        java.sql.ResultSetMetaData metaData = mock(java.sql.ResultSetMetaData.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(0);

        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("title")).thenReturn("Art");
        when(resultSet.getString("description")).thenReturn("Desc");
        when(resultSet.getInt("price")).thenReturn(50);
        when(resultSet.getString("imageurl")).thenReturn("url");
        when(resultSet.getString("pdf_url")).thenReturn("pdf");
        when(resultSet.getString("ai_summary")).thenReturn("ai");
        when(resultSet.getString("status")).thenReturn("status");
        when(resultSet.getInt("category_id")).thenReturn(1);

        List<Artworks> result = serviceArtworks.getAll();

        assertEquals(1, result.size());
        assertEquals("Art", result.get(0).getTitle());
    }

    @Test
    void testAddArtworkThrowsExceptionWhenConnectionIsNull() {
        ServiceArtworks service = new ServiceArtworks(null);
        Artworks artwork = new Artworks();
        assertThrows(SQLException.class, () -> service.add(artwork));
    }
}
