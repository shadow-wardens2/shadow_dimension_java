package Entities.event;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void testEventGettersAndSetters() {
        Event event = new Event();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        event.setId(1);
        event.setTitle("Test Event");
        event.setDescription("Description");
        event.setLocation("Location");
        event.setStartDate(now);
        event.setEndDate(now);
        event.setImage("image.jpg");
        event.setCapacity(100);
        event.setQrCodePath("qr.png");
        event.setCreatedAt(now);
        event.setStatus("ACTIVE");
        event.setCreatedById(2);
        event.setVisualVibe("Dark");
        event.setLocationType("Indoor");

        assertEquals(1, event.getId());
        assertEquals("Test Event", event.getTitle());
        assertEquals("Description", event.getDescription());
        assertEquals("Location", event.getLocation());
        assertEquals(now, event.getStartDate());
        assertEquals(now, event.getEndDate());
        assertEquals("image.jpg", event.getImage());
        assertEquals(100, event.getCapacity());
        assertEquals("qr.png", event.getQrCodePath());
        assertEquals(now, event.getCreatedAt());
        assertEquals("ACTIVE", event.getStatus());
        assertEquals(2, event.getCreatedById());
        assertEquals("Dark", event.getVisualVibe());
        assertEquals("Indoor", event.getLocationType());
    }

    @Test
    void testGetCategoryName() {
        Event event = new Event();
        
        // When category is null
        assertEquals("", event.getCategoryName());

        // When category is set
        Category category = new Category();
        category.setNom("Music");
        event.setCategory(category);
        
        assertEquals("Music", event.getCategoryName());
    }
    
    @Test
    void testEventConstructor() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Category category = new Category();
        category.setNom("Art");

        Event event = new Event(
                10, "Title", "Desc", "Loc", now, now, "img.png", 50, "qr", now, "ACTIVE", category, 1, "Vibe", "Out"
        );

        assertEquals(10, event.getId());
        assertEquals("Title", event.getTitle());
        assertEquals("Desc", event.getDescription());
        assertEquals("Loc", event.getLocation());
        assertEquals(now, event.getStartDate());
        assertEquals(now, event.getEndDate());
        assertEquals("img.png", event.getImage());
        assertEquals(50, event.getCapacity());
        assertEquals("qr", event.getQrCodePath());
        assertEquals(now, event.getCreatedAt());
        assertEquals("ACTIVE", event.getStatus());
        assertEquals(category, event.getCategory());
        assertEquals(1, event.getCreatedById());
        assertEquals("Vibe", event.getVisualVibe());
        assertEquals("Out", event.getLocationType());
    }
}
