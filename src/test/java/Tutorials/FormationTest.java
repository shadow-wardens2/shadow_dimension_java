package Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Jeu;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FormationTest {

    @Test
    public void testFormationSettersAndGetters() {
        Formation formation = new Formation();
        formation.setTitre("JavaFX Mastery");
        formation.setNiveau("expert");

        Jeu jeu = new Jeu();
        jeu.setNom("Shadow Dimensions");
        formation.setJeu(jeu);

        Assertions.assertEquals("JavaFX Mastery", formation.getTitre());
        Assertions.assertEquals("expert", formation.getNiveau());
        Assertions.assertEquals("Shadow Dimensions", formation.getJeu().getNom());
    }
}
