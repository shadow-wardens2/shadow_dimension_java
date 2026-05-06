package Entities.Artworks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArtworksTest {

    @Test
    void testArtworksGettersAndSetters() {
        Artworks artwork = new Artworks();
        artwork.setId(1);
        artwork.setTitle("Starry Night");
        artwork.setDescription("A famous painting by Van Gogh");
        artwork.setPrice(1000);
        artwork.setImageurl("http://example.com/image.jpg");
        artwork.setPdfUrl("http://example.com/file.pdf");
        artwork.setAiSummary("Beautiful night sky");
        artwork.setStatus("Available");
        artwork.setCategoryID(2);

        assertEquals(1, artwork.getId());
        assertEquals("Starry Night", artwork.getTitle());
        assertEquals("A famous painting by Van Gogh", artwork.getDescription());
        assertEquals(1000, artwork.getPrice());
        assertEquals("http://example.com/image.jpg", artwork.getImageurl());
        assertEquals("http://example.com/file.pdf", artwork.getPdfUrl());
        assertEquals("Beautiful night sky", artwork.getAiSummary());
        assertEquals("Available", artwork.getStatus());
        assertEquals(2, artwork.getCategoryID());
    }

    @Test
    void testEqualsAndHashCode() {
        Artworks a1 = new Artworks(1, "Title", "Desc", 100, "img", "pdf", "ai", "status", 1);
        Artworks a2 = new Artworks(1, "Title", "Desc", 100, "img", "pdf", "ai", "status", 1);
        Artworks a3 = new Artworks(2, "Title", "Desc", 100, "img", "pdf", "ai", "status", 1);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, a3);
    }
}
