package Entities.Marketplace;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour l'entité Produit.
 */
@DisplayName("Tests unitaires - Produit")
class ProduitTest {

    private Produit produit;

    @BeforeEach
    void setUp() {
        produit = new Produit(1, "Épée de feu", "Une épée magique enflammée", 49.99, 10, 2, 3, "http://example.com/epee.jpg");
    }

    // ─── Constructeur & Getters ───────────────────────────────────────────────

    @Test
    @DisplayName("Le constructeur complet initialise tous les champs correctement")
    void testConstructeurComplet() {
        assertEquals(1, produit.getId());
        assertEquals("Épée de feu", produit.getNom());
        assertEquals("Une épée magique enflammée", produit.getDescription());
        assertEquals(49.99, produit.getPrix(), 0.001);
        assertEquals(10, produit.getStock());
        assertEquals(2, produit.getCategorieId());
        assertEquals(3, produit.getTypeId());
        assertEquals("http://example.com/epee.jpg", produit.getImage());
    }

    @Test
    @DisplayName("Le constructeur vide crée un produit avec valeurs par défaut")
    void testConstructeurVide() {
        Produit p = new Produit();
        assertEquals(0, p.getId());
        assertNull(p.getNom());
        assertNull(p.getDescription());
        assertEquals(0.0, p.getPrix(), 0.001);
        assertEquals(0, p.getStock());
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setNom() modifie correctement le nom")
    void testSetNom() {
        produit.setNom("Bouclier légendaire");
        assertEquals("Bouclier légendaire", produit.getNom());
    }

    @Test
    @DisplayName("setPrix() modifie correctement le prix")
    void testSetPrix() {
        produit.setPrix(99.99);
        assertEquals(99.99, produit.getPrix(), 0.001);
    }

    @Test
    @DisplayName("setStock() modifie correctement le stock")
    void testSetStock() {
        produit.setStock(50);
        assertEquals(50, produit.getStock());
    }

    @Test
    @DisplayName("setDescription() modifie correctement la description")
    void testSetDescription() {
        produit.setDescription("Nouvelle description");
        assertEquals("Nouvelle description", produit.getDescription());
    }

    @Test
    @DisplayName("setImage() modifie correctement l'URL de l'image")
    void testSetImage() {
        produit.setImage("http://example.com/nouveau.jpg");
        assertEquals("http://example.com/nouveau.jpg", produit.getImage());
    }

    @Test
    @DisplayName("setCategorieId() modifie correctement la catégorie")
    void testSetCategorieId() {
        produit.setCategorieId(5);
        assertEquals(5, produit.getCategorieId());
    }

    @Test
    @DisplayName("setTypeId() modifie correctement le type")
    void testSetTypeId() {
        produit.setTypeId(7);
        assertEquals(7, produit.getTypeId());
    }

    // ─── equals & hashCode ───────────────────────────────────────────────────

    @Test
    @DisplayName("Deux produits avec le même ID sont égaux")
    void testEqualsMemId() {
        Produit p2 = new Produit(1, "Autre nom", "Autre desc", 10.0, 5, 1, 1, null);
        assertEquals(produit, p2);
    }

    @Test
    @DisplayName("Deux produits avec des IDs différents ne sont pas égaux")
    void testEqualsIdDifferent() {
        Produit p2 = new Produit(2, "Épée de feu", "Une épée magique enflammée", 49.99, 10, 2, 3, "http://example.com/epee.jpg");
        assertNotEquals(produit, p2);
    }

    @Test
    @DisplayName("Un produit est égal à lui-même")
    void testEqualsMemeObjet() {
        assertEquals(produit, produit);
    }

    @Test
    @DisplayName("Un produit n'est pas égal à null")
    void testEqualsNull() {
        assertNotEquals(null, produit);
    }

    @Test
    @DisplayName("Deux produits avec le même ID ont le même hashCode")
    void testHashCode() {
        Produit p2 = new Produit(1, "Autre", "...", 0, 0, 0, 0, null);
        assertEquals(produit.hashCode(), p2.hashCode());
    }

    // ─── toString ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString() contient le nom du produit")
    void testToString() {
        String str = produit.toString();
        assertTrue(str.contains("Épée de feu"));
    }

    // ─── Cas limites ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Un prix de 0.0 est stocké correctement")
    void testPrixZero() {
        produit.setPrix(0.0);
        assertEquals(0.0, produit.getPrix(), 0.001);
    }

    @Test
    @DisplayName("Un stock de 0 est stocké correctement")
    void testStockZero() {
        produit.setStock(0);
        assertEquals(0, produit.getStock());
    }

    @Test
    @DisplayName("Une image null est stockée correctement")
    void testImageNull() {
        produit.setImage(null);
        assertNull(produit.getImage());
    }
}
