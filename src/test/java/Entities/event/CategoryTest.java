package Entities.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryTest {

    @Test
    void toString_returnsNom() {
        Category category = new Category();
        category.setNom("Technology");

        assertEquals("Technology", category.toString());
    }

    @Test
    void settersAndGetters_storeCategoryFields() {
        Category category = new Category();

        category.setId(12);
        category.setNom("Gaming");
        category.setDescription("Events for gamers");
        category.setTypeTarification("PAID");
        category.setPrix(89.9);
        category.setCreatorType("SYSTEM");

        assertEquals(12, category.getId());
        assertEquals("Gaming", category.getNom());
        assertEquals("Events for gamers", category.getDescription());
        assertEquals("PAID", category.getTypeTarification());
        assertEquals(89.9, category.getPrix());
        assertEquals("SYSTEM", category.getCreatorType());
    }
}
