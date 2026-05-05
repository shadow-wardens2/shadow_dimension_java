package Entities.Artworks;

import java.util.List;

/**
 * Holds the full AI price analysis result returned by GeminiDescriptionService.
 */
public class PriceAnalysis {

    // ------------------------------------------------------------------
    // Criterion inner class
    // ------------------------------------------------------------------

    public static class Criterion {
        public final String name;
        public final int    score;   // 0-100: how well this artwork scores on this criterion
        public final int    weight;  // % weight this criterion contributes to the final price
        public final String note;    // brief AI explanation

        public Criterion(String name, int score, int weight, String note) {
            this.name   = name;
            this.score  = score;
            this.weight = weight;
            this.note   = note;
        }
    }

    // ------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------

    public final int             price;         // suggested USD price
    public final String          marketInsight; // 2-3 sentence market trend analysis
    public final List<Criterion> criteria;      // ordered list of scored criteria

    public PriceAnalysis(int price, String marketInsight, List<Criterion> criteria) {
        this.price         = price;
        this.marketInsight = marketInsight;
        this.criteria      = criteria;
    }
}
