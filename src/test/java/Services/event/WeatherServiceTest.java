package Services.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherServiceTest {

    @Test
    void getForecastWithNullLocationThrowsException() {
        WeatherService weatherService = new WeatherService();
        
        IOException exception = assertThrows(IOException.class, () -> {
            weatherService.getForecast(null);
        });
        
        assertEquals("Location is required.", exception.getMessage());
    }

    @Test
    void getForecastWithBlankLocationThrowsException() {
        WeatherService weatherService = new WeatherService();
        
        IOException exception = assertThrows(IOException.class, () -> {
            weatherService.getForecast("   ");
        });
        
        assertEquals("Location is required.", exception.getMessage());
    }
}
