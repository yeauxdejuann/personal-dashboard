package com.dashboard.app.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * QuoteService fetches inspirational quotes from the Quotable API
 * 
 * This service demonstrates:
 * - Working with a simple, single-object JSON response
 * - Error handling with fallback data
 * - Filtering API results with query parameters
 */
@Service
public class QuoteService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public QuoteService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Gets a random inspirational quote
     * 
     * Unlike the weather service that needs two API calls, this is simpler:
     * - Make one HTTP call
     * - Parse the JSON response (single object, not an array)
     * - Extract the quote content and author
     * 
     * @return QuoteData with inspirational quote and author
     */
    public QuoteData getRandomQuote() {
        try {
            // API URL with filters for inspirational content
            // The "tags" parameter filters to only motivational quotes
            String url = "https://api.quotable.io/random?tags=inspirational|motivational|wisdom|success";
            
            // Make HTTP call - this API returns a single JSON object, not an array
            String response = restTemplate.getForObject(url, String.class);
            
            // Parse the JSON response
            JsonNode json = objectMapper.readTree(response);
            
            // Extract data from JSON - much simpler than the weather API!
            // The JSON looks like: {"content": "Quote text", "author": "Author Name", ...}
            String content = json.get("content").asText();
            String author = json.get("author").asText();
            
            return new QuoteData(content, author);
            
        } catch ( Exception e) {
            // If the API is down or there's an error, return a hardcoded fallback quote
            // This ensures our dashboard always has content, even when external APIs fail
            System.out.println("Error fetching quote: " + e.getMessage());
            return new QuoteData(
                "The only way to do great work is to love what you do.",
                "Steve Jobs"
            );
        }
    }
    
    /**
     * QuoteData record - much simpler than WeatherData
     * Only needs two fields: the quote text and who said it
     */
    public record QuoteData(
        String content,  // The actual quote text
        String author    // Who said it
    ) {}
}