package Tutorials;

import Entities.Tutorials.Jeu;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JeuTest {
    @Test
    public void testJeuSettersAndGetters() {
        Jeu jeu = new Jeu();
        jeu.setId(1);
        jeu.setNom("Shadow Dimensions");
        jeu.setGenre("Aventure/Action");

        Assertions.assertEquals(1, jeu.getId());
        Assertions.assertEquals("Shadow Dimensions", jeu.getNom());
        Assertions.assertEquals("Aventure/Action", jeu.getGenre());
    }
}
