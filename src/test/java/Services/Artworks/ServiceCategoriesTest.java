package Services.Artworks;

import Entities.Artworks.Categories;
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
class ServiceCategoriesTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private ServiceCategories serviceCategories;

    @BeforeEach
    void setUp() {
        serviceCategories = new ServiceCategories(connection);
    }

    @Test
    void testAddCategory() throws SQLException {
        Categories category = new Categories("CAT1", "Paintings", "Desc");
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceCategories.add(category);

        verify(preparedStatement).setString(1, "Paintings");
        verify(preparedStatement).setString(2, "Desc");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testUpdateCategory() throws SQLException {
        Categories category = new Categories("CAT1", "Updated", "Desc");
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceCategories.update(category);

        verify(preparedStatement).setString(1, "Updated");
        verify(preparedStatement).setString(3, "CAT1");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testDeleteCategory() throws SQLException {
        Categories category = new Categories("CAT1", "Paintings", "Desc");
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        serviceCategories.delete(category);

        verify(preparedStatement).setString(1, "CAT1");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testGetAll() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("id")).thenReturn("CAT1");
        when(resultSet.getString("name")).thenReturn("Paintings");
        when(resultSet.getString("description")).thenReturn("Desc");

        List<Categories> result = serviceCategories.getAll();

        assertEquals(1, result.size());
        assertEquals("Paintings", result.get(0).getTitle());
    }
}
