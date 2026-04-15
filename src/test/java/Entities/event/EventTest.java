package Entities.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTest {

    @Test
    void getCategoryName_returnsEmptyWhenCategoryMissing() {
        Event event = new Event();

        assertEquals("", event.getCategoryName());
    }

    @Test
    void getCategoryName_returnsCategoryNomWhenPresent() {
        Category category = new Category();
        category.setNom("Music");

        Event event = new Event();
        event.setCategory(category);

        assertEquals("Music", event.getCategoryName());
    }

    @Test
    void settersAndGetters_storeCoreEventFields() {
        Event event = new Event();

        event.setId(42);
        event.setTitle("Night Festival");
        event.setDescription("Live show in shadow arena");
        event.setLocation("Tunis");
        event.setImage("https://example.com/festival.png");
        event.setCapacity(350);
        event.setStatus("ACTIVE");
        event.setLocationType("outdoor");
        event.setCreatedById(7);

        assertEquals(42, event.getId());
        assertEquals("Night Festival", event.getTitle());
        assertEquals("Live show in shadow arena", event.getDescription());
        assertEquals("Tunis", event.getLocation());
        assertEquals("https://example.com/festival.png", event.getImage());
        assertEquals(350, event.getCapacity());
        assertEquals("ACTIVE", event.getStatus());
        assertEquals("outdoor", event.getLocationType());
        assertEquals(7, event.getCreatedById());
    }
}
