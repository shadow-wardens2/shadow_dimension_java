package Services.Marketplace;

import Entities.Marketplace.Categorie;
import Utils.ShadowDimensionsDB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("ServiceCategorie Unit Tests")
public class ServiceCategorieTest {

    private Connection mockConnection;
    private ShadowDimensionsDB mockDb;
    private MockedStatic<ShadowDimensionsDB> mockedStaticDb;

    @BeforeEach
    void setUp() {
        mockConnection = mock(Connection.class);
        mockDb = mock(ShadowDimensionsDB.class);
        when(mockDb.getConnection()).thenReturn(mockConnection);
        
        mockedStaticDb = mockStatic(ShadowDimensionsDB.class);
        mockedStaticDb.when(ShadowDimensionsDB::getInstance).thenReturn(mockDb);
    }

    @AfterEach
    void tearDown() {
        if (mockedStaticDb != null) {
            mockedStaticDb.close();
        }
    }

    @Test
    @DisplayName("Test getAll categories")
    void testGetAll() throws Exception {
        Statement mockStatement = mock(Statement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(101);
        when(mockResultSet.getString("nom")).thenReturn("Relics");
        when(mockResultSet.getString("description")).thenReturn("Ancient artifacts");

        ServiceCategorie service = new ServiceCategorie();
        List<Categorie> categories = service.getAll();

        assertEquals(1, categories.size());
        assertEquals("Relics", categories.get(0).getNom());
    }
}
