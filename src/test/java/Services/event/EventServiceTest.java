package Services.event;

import Entities.event.Category;
import Entities.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventServiceTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private PreparedStatement reclamationDeleteStatement;
    private PreparedStatement reviewDeleteStatement;
    private PreparedStatement reservationDeleteStatement;
    private PreparedStatement eventDeleteStatement;
    private Statement statement;
    private ResultSet resultSet;
    private EventService eventService;

    @BeforeEach
    void setUp() throws Exception {
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        reclamationDeleteStatement = mock(PreparedStatement.class);
        reviewDeleteStatement = mock(PreparedStatement.class);
        reservationDeleteStatement = mock(PreparedStatement.class);
        eventDeleteStatement = mock(PreparedStatement.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.getAutoCommit()).thenReturn(true);
        
        eventService = new EventService(connection);
    }

    @Test
    void addEventSuccessfully() throws Exception {
        Category category = new Category();
        category.setId(2);
        
        Event event = new Event();
        event.setTitle("Festival");
        event.setDescription("Music festival");
        event.setLocation("Park");
        event.setStartDate(new Timestamp(System.currentTimeMillis()));
        event.setEndDate(new Timestamp(System.currentTimeMillis() + 86400000));
        event.setImage("image.png");
        event.setCapacity(500);
        event.setQrCodePath("qr.png");
        event.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        event.setStatus("ACTIVE");
        event.setCategory(category);
        event.setCreatedById(1);
        event.setVisualVibe("Energetic");
        event.setLocationType("Outdoor");

        eventService.add(event);

        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).setString(1, "Festival");
        verify(preparedStatement).setString(2, "Music festival");
        verify(preparedStatement).setString(3, "Park");
        verify(preparedStatement).setInt(7, 500);
        verify(preparedStatement).setString(10, "ACTIVE");
        verify(preparedStatement).setInt(11, 2);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void updateEventSuccessfully() throws Exception {
        Category category = new Category();
        category.setId(3);

        Event event = new Event();
        event.setId(10);
        event.setTitle("Updated Title");
        event.setDescription("Updated Desc");
        event.setLocation("Hall");
        event.setStartDate(new Timestamp(System.currentTimeMillis()));
        event.setEndDate(new Timestamp(System.currentTimeMillis() + 86400000));
        event.setImage("new_image.png");
        event.setCapacity(100);
        event.setQrCodePath(null);
        event.setStatus("CANCELLED");
        event.setCategory(category);
        event.setCreatedById(2);
        event.setVisualVibe("Chill");
        event.setLocationType("Indoor");

        eventService.update(event);

        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).setString(1, "Updated Title");
        verify(preparedStatement).setNull(8, java.sql.Types.VARCHAR);
        verify(preparedStatement).setString(9, "CANCELLED");
        verify(preparedStatement).setInt(10, 3);
        verify(preparedStatement).setInt(14, 10); // ID
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void deleteEventSuccessfully() throws Exception {
        Event event = new Event();
        event.setId(7);

        when(connection.prepareStatement(eq("DELETE FROM evt_reclamation WHERE event_id=?"))).thenReturn(reclamationDeleteStatement);
        when(connection.prepareStatement(eq("DELETE FROM evt_review WHERE event_id=?"))).thenReturn(reviewDeleteStatement);
        when(connection.prepareStatement(eq("DELETE FROM evt_reservation WHERE event_id=?"))).thenReturn(reservationDeleteStatement);
        when(connection.prepareStatement(eq("DELETE FROM evt_event WHERE id=?"))).thenReturn(eventDeleteStatement);

        eventService.delete(event);

        InOrder inOrder = inOrder(connection, reclamationDeleteStatement, reviewDeleteStatement, reservationDeleteStatement, eventDeleteStatement);
        inOrder.verify(connection).setAutoCommit(false);
        inOrder.verify(connection).prepareStatement("DELETE FROM evt_reclamation WHERE event_id=?");
        inOrder.verify(reclamationDeleteStatement).setInt(1, 7);
        inOrder.verify(reclamationDeleteStatement).executeUpdate();
        inOrder.verify(connection).prepareStatement("DELETE FROM evt_review WHERE event_id=?");
        inOrder.verify(reviewDeleteStatement).setInt(1, 7);
        inOrder.verify(reviewDeleteStatement).executeUpdate();
        inOrder.verify(connection).prepareStatement("DELETE FROM evt_reservation WHERE event_id=?");
        inOrder.verify(reservationDeleteStatement).setInt(1, 7);
        inOrder.verify(reservationDeleteStatement).executeUpdate();
        inOrder.verify(connection).prepareStatement("DELETE FROM evt_event WHERE id=?");
        inOrder.verify(eventDeleteStatement).setInt(1, 7);
        inOrder.verify(eventDeleteStatement).executeUpdate();
        inOrder.verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void getAllEventsReturnsList() throws Exception {
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("title")).thenReturn("Concert");
        when(resultSet.getInt("category_id")).thenReturn(5);
        when(resultSet.getString("category_name")).thenReturn("Music");

        List<Event> events = eventService.getAll();

        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("Concert", events.get(0).getTitle());
        assertEquals("Music", events.get(0).getCategory().getNom());
        verify(statement).executeQuery(anyString());
    }

    @Test
    void getByIdReturnsEvent() throws Exception {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        
        when(resultSet.getInt("id")).thenReturn(5);
        when(resultSet.getString("title")).thenReturn("Workshop");
        when(resultSet.getInt("category_id")).thenReturn(2);
        when(resultSet.getString("category_name")).thenReturn("Education");

        Event result = eventService.getById(5);

        assertNotNull(result);
        assertEquals(5, result.getId());
        assertEquals("Workshop", result.getTitle());
        assertEquals(2, result.getCategory().getId());
        
        verify(preparedStatement).setInt(1, 5);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void getEventCountByCategoryWorks() throws Exception {
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        
        when(resultSet.getString("category_name")).thenReturn("Music", "Art");
        when(resultSet.getInt("total")).thenReturn(10, 5);

        Map<String, Integer> counts = eventService.getEventCountByCategory();

        assertNotNull(counts);
        assertEquals(2, counts.size());
        assertEquals(10, counts.get("Music"));
        assertEquals(5, counts.get("Art"));
    }
}
