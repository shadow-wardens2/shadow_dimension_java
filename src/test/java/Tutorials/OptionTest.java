package Tutorials;

import Entities.Tutorials.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OptionTest {
    @Test
    public void testOptionSettersAndGetters() {
        Option option = new Option();
        option.setTexte("Paris");
        option.setEstCorrecte(true);

        Assertions.assertEquals("Paris", option.getTexte());
        Assertions.assertTrue(option.isEstCorrecte());
    }
}
