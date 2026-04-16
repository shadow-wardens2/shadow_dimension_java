package Entities.Marketplace;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour l'entité Categorie.
 */
@DisplayName("Tests unitaires - Categorie")
class CategorieTest {

    private Categorie categorie;

    @BeforeEach
    void setUp() {
        categorie = new Categorie(1, "Armes", "Toutes les armes du jeu");
    }

    // ─── Constructeur & Getters ───────────────────────────────────────────────

    @Test
    @DisplayName("Le constructeur complet initialise tous les champs correctement")
    void testConstructeurComplet() {
        assertEquals(1, categorie.getId());
        assertEquals("Armes", categorie.getNom());
        assertEquals("Toutes les armes du jeu", categorie.getDescription());
    }

    @Test
    @DisplayName("Le constructeur vide crée une catégorie avec valeurs par défaut")
    void testConstructeurVide() {
        Categorie c = new Categorie();
        assertEquals(0, c.getId());
        assertNull(c.getNom());
        assertNull(c.getDescription());
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setId() modifie correctement l'identifiant")
    void testSetId() {
        categorie.setId(99);
        assertEquals(99, categorie.getId());
    }

    @Test
    @DisplayName("setNom() modifie correctement le nom")
    void testSetNom() {
        categorie.setNom("Armures");
        assertEquals("Armures", categorie.getNom());
    }

    @Test
    @DisplayName("setDescription() modifie correctement la description")
    void testSetDescription() {
        categorie.setDescription("Nouvelle description");
        assertEquals("Nouvelle description", categorie.getDescription());
    }

    // ─── toString ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString() retourne le nom de la catégorie")
    void testToString() {
        assertEquals("Armes", categorie.toString());
    }

    @Test
    @DisplayName("toString() avec nom null retourne null")
    void testToStringNomNull() {
        Categorie c = new Categorie();
        assertNull(c.toString());
    }

    // ─── Cas limites ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Un nom vide est stocké correctement")
    void testNomVide() {
        categorie.setNom("");
        assertEquals("", categorie.getNom());
    }

    @Test
    @DisplayName("Une description null est stockée correctement")
    void testDescriptionNull() {
        categorie.setDescription(null);
        assertNull(categorie.getDescription());
    }

    @Test
    @DisplayName("Un ID de 0 (non persisté) est valide")
    void testIdZero() {
        categorie.setId(0);
        assertEquals(0, categorie.getId());
    }

    @Test
    @DisplayName("Un ID négatif est stocké tel quel (pas de validation dans l'entité)")
    void testIdNegatif() {
        categorie.setId(-5);
        assertEquals(-5, categorie.getId());
    }

    @Test
    @DisplayName("Deux catégories distinctes peuvent avoir le même nom")
    void testDeuxCategoriesMemeNom() {
        Categorie c2 = new Categorie(2, "Armes", "Une autre description");
        assertEquals(categorie.getNom(), c2.getNom());
        assertNotEquals(categorie.getId(), c2.getId());
    }
}
