package Entities.Marketplace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Marketplace Entity Tests")
public class MarketplaceEntityTest {

    @Test
    @DisplayName("Produit basic properties and constructor")
    void testProduitProperties() {
        Produit p = new Produit(1, "Ethereal Blade", "A blade made of pure shadow", 299.99, 5, 101, 1, "blade.png");
        
        assertEquals(1, p.getId());
        assertEquals("Ethereal Blade", p.getNom());
        assertEquals("A blade made of pure shadow", p.getDescription());
        assertEquals(299.99, p.getPrix());
        assertEquals(5, p.getStock());
        assertEquals(101, p.getCategorieId());
        assertEquals(1, p.getTypeId());
        assertEquals("blade.png", p.getImage());
    }

    @Test
    @DisplayName("Categorie basic properties")
    void testCategorieProperties() {
        Categorie c = new Categorie(1, "Weapons", "Battle-ready artifacts");
        
        assertEquals(1, c.getId());
        assertEquals("Weapons", c.getNom());
        assertEquals("Battle-ready artifacts", c.getDescription());
    }

    @Test
    @DisplayName("Type basic properties")
    void testTypeProperties() {
        Type t = new Type(1, "Mythic");
        
        assertEquals(1, t.getId());
        assertEquals("Mythic", t.getNom());
    }

    @Test
    @DisplayName("Produit setters")
    void testProduitSetters() {
        Produit p = new Produit();
        p.setNom("Void Orb");
        p.setStock(10);
        
        assertEquals("Void Orb", p.getNom());
        assertEquals(10, p.getStock());
    }
}
