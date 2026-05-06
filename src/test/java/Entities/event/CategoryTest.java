package Entities.event;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    void testCategoryGettersAndSetters() {
        Category category = new Category();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        category.setId(1);
        category.setNom("Music");
        category.setDescription("Live Concerts");
        category.setTypeTarification("Paid");
        category.setPrix(50.5);
        category.setCreatorType("USER");
        category.setCreatedAt(now);

        assertEquals(1, category.getId());
        assertEquals("Music", category.getNom());
        assertEquals("Live Concerts", category.getDescription());
        assertEquals("Paid", category.getTypeTarification());
        assertEquals(50.5, category.getPrix());
        assertEquals("USER", category.getCreatorType());
        assertEquals(now, category.getCreatedAt());
    }

    @Test
    void testCategoryToString() {
        Category category = new Category();
        category.setNom("Art Exhibition");
        
        assertEquals("Art Exhibition", category.toString());
    }

    @Test
    void testCategoryConstructor() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Category category = new Category(2, "Gaming", "Esports", "Free", null, "ADMIN", now);

        assertEquals(2, category.getId());
        assertEquals("Gaming", category.getNom());
        assertEquals("Esports", category.getDescription());
        assertEquals("Free", category.getTypeTarification());
        assertNull(category.getPrix());
        assertEquals("ADMIN", category.getCreatorType());
        assertEquals(now, category.getCreatedAt());
    }
}
