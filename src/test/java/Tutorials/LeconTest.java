package Tutorials;

import Entities.Tutorials.Lecon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LeconTest {
    @Test
    public void testLeconSettersAndGetters() {
        Lecon lecon = new Lecon();
        lecon.setTitre("Introduction aux Jeux Vidéo");
        lecon.setContenu("Ceci est le contenu de la leçon.");
        lecon.setOrdre(1);

        Assertions.assertEquals("Introduction aux Jeux Vidéo", lecon.getTitre());
        Assertions.assertEquals("Ceci est le contenu de la leçon.", lecon.getContenu());
        Assertions.assertEquals(1, lecon.getOrdre());
    }
}
