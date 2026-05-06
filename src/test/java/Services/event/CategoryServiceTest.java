package Services.event;

import Entities.event.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private Statement statement;
    private ResultSet resultSet;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() throws Exception {
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.createStatement()).thenReturn(statement);
        
        categoryService = new CategoryService(connection);
    }

    @Test
    void addCategorySuccessfully() throws Exception {
        Category category = new Category(0, "Music", "Music Event", "Paid", 50.0, "USER", new Timestamp(System.currentTimeMillis()));

        categoryService.add(category);

        verify(connection).prepareStatement("INSERT INTO evt_category(nom, description, type_tarification, prix, creator_type, created_at) VALUES (?, ?, ?, ?, ?, ?)");
        verify(preparedStatement).setString(1, "Music");
        verify(preparedStatement).setString(2, "Music Event");
        verify(preparedStatement).setString(3, "Paid");
        verify(preparedStatement).setDouble(4, 50.0);
        verify(preparedStatement).setString(5, "USER");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void updateCategorySuccessfully() throws Exception {
        Category category = new Category(1, "Updated", "Updated Desc", "Free", null, "ADMIN", new Timestamp(System.currentTimeMillis()));

        categoryService.update(category);

        verify(connection).prepareStatement("UPDATE evt_category SET nom=?, description=?, type_tarification=?, prix=?, creator_type=? WHERE id=?");
        verify(preparedStatement).setString(1, "Updated");
        verify(preparedStatement).setString(2, "Updated Desc");
        verify(preparedStatement).setString(3, "Free");
        verify(preparedStatement).setNull(4, java.sql.Types.DECIMAL);
        verify(preparedStatement).setString(5, "ADMIN");
        verify(preparedStatement).setInt(6, 1);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void deleteCategorySuccessfully() throws Exception {
        Category category = new Category();
        category.setId(5);

        categoryService.delete(category);

        verify(connection).prepareStatement("DELETE FROM evt_category WHERE id=?");
        verify(preparedStatement).setInt(1, 5);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void getAllCategoriesReturnsList() throws Exception {
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("nom")).thenReturn("Music");
        when(resultSet.getString("description")).thenReturn("Desc");
        when(resultSet.getString("type_tarification")).thenReturn("Free");
        when(resultSet.getObject("prix")).thenReturn(null);
        when(resultSet.getString("creator_type")).thenReturn("SYSTEM");
        when(resultSet.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));

        List<Category> categories = categoryService.getAll();

        assertNotNull(categories);
        assertEquals(1, categories.size());
        assertEquals("Music", categories.get(0).getNom());
        verify(statement).executeQuery("SELECT * FROM evt_category ORDER BY id DESC");
    }

    @Test
    void getByIdReturnsCategory() throws Exception {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        
        when(resultSet.getInt("id")).thenReturn(3);
        when(resultSet.getString("nom")).thenReturn("Art");
        when(resultSet.getString("description")).thenReturn("Art exhibition");
        when(resultSet.getString("type_tarification")).thenReturn("Paid");
        when(resultSet.getObject("prix")).thenReturn(10.0);
        when(resultSet.getDouble("prix")).thenReturn(10.0);
        when(resultSet.getString("creator_type")).thenReturn("USER");

        Category result = categoryService.getById(3);

        assertNotNull(result);
        assertEquals(3, result.getId());
        assertEquals("Art", result.getNom());
        assertEquals(10.0, result.getPrix());
        
        verify(preparedStatement).setInt(1, 3);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void getByIdReturnsNullWhenNotFound() throws Exception {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Category result = categoryService.getById(99);

        assertNull(result);
    }
}
