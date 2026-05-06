package Entities.Artworks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CategoriesTest {

    @Test
    void testCategoriesGettersAndSetters() {
        Categories category = new Categories();
        category.setID("CAT1");
        category.setTitle("Paintings");
        category.setDescription("Hand-drawn paintings");

        assertEquals("CAT1", category.getID());
        assertEquals("Paintings", category.getTitle());
        assertEquals("Hand-drawn paintings", category.getDescription());
    }

    @Test
    void testToString() {
        Categories category = new Categories("CAT1", "Paintings", "Desc");
        assertEquals("Paintings", category.toString());
        
        Categories nullTitle = new Categories();
        assertEquals("Unknown", nullTitle.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        Categories c1 = new Categories("CAT1", "Paintings", "Desc");
        Categories c2 = new Categories("CAT1", "Paintings", "Desc");
        Categories c3 = new Categories("CAT2", "Paintings", "Desc");

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);
    }
}
