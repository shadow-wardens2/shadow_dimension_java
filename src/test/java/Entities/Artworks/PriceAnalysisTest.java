package Entities.Artworks;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PriceAnalysisTest {

    @Test
    void testPriceAnalysisCreation() {
        PriceAnalysis.Criterion c1 = new PriceAnalysis.Criterion("Complexity", 80, 50, "Very complex");
        PriceAnalysis.Criterion c2 = new PriceAnalysis.Criterion("Style", 90, 50, "Unique style");
        List<PriceAnalysis.Criterion> criteria = Arrays.asList(c1, c2);
        
        PriceAnalysis analysis = new PriceAnalysis(1500, "Good market trends", criteria);

        assertEquals(1500, analysis.price);
        assertEquals("Good market trends", analysis.marketInsight);
        assertEquals(2, analysis.criteria.size());
        assertEquals("Complexity", analysis.criteria.get(0).name);
    }
}
