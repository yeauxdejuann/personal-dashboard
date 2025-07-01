package com.dashboard.app.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WeatherService fetches real-time weather data using the Open-Meteo API
 * 
 * This service demonstrates:
 * - Making HTTP calls to external APIs
 * - Parsing JSON responses
 * - Handling errors gracefully
 * - Using two APIs together (geocoding + weather)
 */
@Service
public class WeatherService {
    
    // RestTemplate makes HTTP calls to external APIs
    private final RestTemplate restTemplate;
    
    // ObjectMapper converts JSON strings to Java objects
    private final ObjectMapper objectMapper;
    
    /**
     * Constructor - Spring automatically creates this service
     */
    public WeatherService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Gets current weather for any city in the world
     * 
     * This method does two API calls:
     * 1. Get coordinates for the city name (geocoding)
     * 2. Get weather data using those coordinates
     * 
     * @param cityName The city to get weather for (e.g. "London", "New York")
     * @return WeatherData object with temperature, description, etc.
     */
    public WeatherData getCurrentWeather(String cityName) {
        try {
            // STEP 1: Get coordinates for the city
            // We need lat/lon because the weather API requires coordinates
            String geocodeUrl = String.format(
                "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json", 
                cityName.replace(" ", "%20") // Handle spaces in city names like "New York"
            );
            
            // Make the HTTP call and get JSON response as a string
            String geocodeResponse = restTemplate.getForObject(geocodeUrl, String.class);
            
            // Parse the JSON string into a tree structure we can navigate
            JsonNode geocodeJson = objectMapper.readTree(geocodeResponse);
            
            // Check if we found any results
            if (!geocodeJson.has("results") || geocodeJson.get("results").isEmpty()) {
                return new WeatherData("City not found", "N/A", 0.0, "N/A");
            }
            
            // Extract coordinates from the first result
            JsonNode firstResult = geocodeJson.get("results").get(0);
            double latitude = firstResult.get("latitude").asDouble();
            double longitude = firstResult.get("longitude").asDouble();
            String countryCode = firstResult.get("country_code").asText();
            
            // STEP 2: Get weather data using the coordinates
            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current_weather=true&timezone=auto",
                latitude, longitude
            );
            
            // Make the second HTTP call for weather data
            String weatherResponse = restTemplate.getForObject(weatherUrl, String.class);
            JsonNode weatherJson = objectMapper.readTree(weatherResponse);
            
            // Extract weather information from the JSON
            JsonNode currentWeather = weatherJson.get("current_weather");
            double temperature = currentWeather.get("temperature").asDouble();
            double windSpeed = currentWeather.get("windspeed").asDouble();
            int weatherCode = currentWeather.get("weathercode").asInt();
            
            // Convert the numeric weather code to a human-readable description
            String description = convertWeatherCode(weatherCode);
            
            // Create and return our weather data object
            return new WeatherData(
                cityName + ", " + countryCode.toUpperCase(), // "London, UK"
                description,                                  // "Partly cloudy"
                temperature,                                  // 15.2
                String.format("%.1f km/h", windSpeed)       // "12.5 km/h"
            );
            
        } catch ( Exception e) {
            // If anything goes wrong (network error, bad JSON, etc.), 
            // return a friendly error message instead of crashing
            System.out.println("Error fetching weather for " + cityName + ": " + e.getMessage());
            return new WeatherData("Error loading weather", "Unable to fetch data", 0.0, "N/A");
        }
    }
    
    /**
     * Converts numeric weather codes to human-readable descriptions
     * Open-Meteo uses WMO weather codes: https://open-meteo.com/en/docs#weathervariables
     */
    private String convertWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Light rain";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 95 -> "Thunderstorm";
            default -> "Unknown weather";
        };
    }
    
    /**
     * WeatherData record holds all the weather information
     * Records are perfect for data that doesn't change (immutable)
     * 
     * Java automatically creates:
     * - Constructor: new WeatherData("London", "Sunny", 20.0, "5 km/h")
     * - Getters: weather.location(), weather.temperature(), etc.
     * - toString(), equals(), hashCode() methods
     */
    public record WeatherData(
        String location,     // "London, UK"
        String description,  // "Partly cloudy"
        double temperature,  // 15.2 (in Celsius)
        String windSpeed     // "12.5 km/h"
    ) {}
}