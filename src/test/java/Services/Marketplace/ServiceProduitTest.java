package Services.Marketplace;

import Entities.Marketplace.Produit;
import Utils.ShadowDimensionsDB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("ServiceProduit Unit Tests")
public class ServiceProduitTest {

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
    @DisplayName("Test getAll products")
    void testGetAll() throws Exception {
        Statement mockStatement = mock(Statement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        
        // Mock two products
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("nom")).thenReturn("Product A", "Product B");
        when(mockResultSet.getDouble("prix")).thenReturn(10.0, 20.0);
        when(mockResultSet.getInt("stock")).thenReturn(5, 10);

        ServiceProduit service = new ServiceProduit();
        List<Produit> products = service.getAll();

        assertEquals(2, products.size());
        assertEquals("Product A", products.get(0).getNom());
        assertEquals("Product B", products.get(1).getNom());
    }

    @Test
    @DisplayName("Test add product")
    void testAdd() throws Exception {
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        Produit p = new Produit(1, "New Item", "Desc", 50.0, 100, 1, 1, "img.png");
        ServiceProduit service = new ServiceProduit();
        service.add(p);

        verify(mockPreparedStatement, times(1)).executeUpdate();
        verify(mockPreparedStatement).setString(1, "New Item");
        verify(mockPreparedStatement).setDouble(3, 50.0);
    }
}
