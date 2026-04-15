package Entities.Marketplace;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour l'entité Type.
 */
@DisplayName("Tests unitaires - Type")
class TypeTest {

    private Type type;

    @BeforeEach
    void setUp() {
        type = new Type(1, "Magique");
    }

    // ─── Constructeur & Getters ───────────────────────────────────────────────

    @Test
    @DisplayName("Le constructeur complet initialise tous les champs correctement")
    void testConstructeurComplet() {
        assertEquals(1, type.getId());
        assertEquals("Magique", type.getNom());
    }

    @Test
    @DisplayName("Le constructeur vide crée un type avec valeurs par défaut")
    void testConstructeurVide() {
        Type t = new Type();
        assertEquals(0, t.getId());
        assertNull(t.getNom());
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setId() modifie correctement l'identifiant")
    void testSetId() {
        type.setId(42);
        assertEquals(42, type.getId());
    }

    @Test
    @DisplayName("setNom() modifie correctement le nom")
    void testSetNom() {
        type.setNom("Physique");
        assertEquals("Physique", type.getNom());
    }

    // ─── toString ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString() retourne le nom du type")
    void testToString() {
        assertEquals("Magique", type.toString());
    }

    @Test
    @DisplayName("toString() avec nom null retourne null")
    void testToStringNomNull() {
        Type t = new Type();
        assertNull(t.toString());
    }

    // ─── Cas limites ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Un nom vide est stocké correctement")
    void testNomVide() {
        type.setNom("");
        assertEquals("", type.getNom());
    }

    @Test
    @DisplayName("Un nom null est stocké correctement")
    void testNomNull() {
        type.setNom(null);
        assertNull(type.getNom());
    }

    @Test
    @DisplayName("Un ID de 0 est valide")
    void testIdZero() {
        type.setId(0);
        assertEquals(0, type.getId());
    }

    @Test
    @DisplayName("Deux types distincts peuvent avoir le même nom")
    void testDeuxTypesMemeNom() {
        Type t2 = new Type(2, "Magique");
        assertEquals(type.getNom(), t2.getNom());
        assertNotEquals(type.getId(), t2.getId());
    }

    @Test
    @DisplayName("Le nom est mis à jour plusieurs fois correctement")
    void testSetNomMultiple() {
        type.setNom("Élémentaire");
        type.setNom("Légendaire");
        assertEquals("Légendaire", type.getNom());
    }
}
