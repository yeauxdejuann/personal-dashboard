package com.dashboard.app.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * CountryService fetches detailed country information from REST Countries API
 * 
 * This service demonstrates:
 * - Working with complex JSON objects
 * - Handling API responses that return arrays
 * - Extracting nested data from JSON
 * - Working with Lists and multiple values
 */
@Service
public class CountryService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public CountryService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Gets comprehensive information about a country
     * 
     * This API is more complex than the quote API:
     * - Returns an array of countries (we take the first match)
     * - Has nested JSON objects (name.common, currencies.USD.name, etc.)
     * - Contains arrays of values (languages, currencies)
     * 
     * @param countryName Name of country to look up
     * @return CountryData with flag, population, languages, etc.
     */
    public CountryData getCountryInfo(String countryName) {
        try {
            // Build URL - the API searches for countries by name
            String url = String.format(
                "https://restcountries.com/v3.1/name/%s?fullText=false", 
                countryName.replace(" ", "%20")
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);
            
            // This API returns an ARRAY of countries, not a single object
            // We need to check if we got results and take the first one
            if (json.isArray() && json.size() > 0) {
                JsonNode country = json.get(0);  // Get first country from the array
                
                // Extract basic info - note the nested structure
                // JSON: {"name": {"common": "United States"}}
                String name = country.get("name").get("common").asText();
                
                // Capital might not exist or might be an array
                // JSON: {"capital": ["Washington D.C."]}
                String capital = "N/A";
                if (country.has("capital") && country.get("capital").isArray()) {
                    capital = country.get("capital").get(0).asText();
                }
                
                // Simple numeric values
                long population = country.get("population").asLong();
                String region = country.get("region").asText();
                String subregion = country.has("subregion") ? 
                    country.get("subregion").asText() : "N/A";
                
                // Extract currencies - this is complex nested data
                // JSON: {"currencies": {"USD": {"name": "United States dollar"}}}
                List<String> currencies = new ArrayList<>();
                if (country.has("currencies")) {
                    JsonNode currenciesNode = country.get("currencies");
                    // Loop through all currency codes (USD, EUR, etc.)
                    currenciesNode.fieldNames().forEachRemaining(currencyCode -> {
                        JsonNode currency = currenciesNode.get(currencyCode);
                        String currencyName = currency.get("name").asText();
                        currencies.add(String.format("%s (%s)", currencyName, currencyCode));
                    });
                }
                
                // Extract languages - similar to currencies
                // JSON: {"languages": {"eng": "English", "spa": "Spanish"}}
                List<String> languages = new ArrayList<>();
                if (country.has("languages")) {
                    JsonNode languagesNode = country.get("languages");
                    languagesNode.fieldNames().forEachRemaining(langCode -> {
                        String langName = languagesNode.get(langCode).asText();
                        languages.add(langName);
                    });
                }
                
                // Get flag image URL
                String flagUrl = "";
                if (country.has("flags")) {
                    flagUrl = country.get("flags").get("png").asText();
                }
                
                // Create and return our country data object
                return new CountryData(
                    name, capital, population, region, subregion,
                    currencies, languages, flagUrl
                );
            }
            
        } catch ( Exception e) {
            System.out.println("Error fetching country info for " + countryName + ": " + e.getMessage());
        }
        
        // Return error state if anything went wrong
        return new CountryData(
            "Country not found", "N/A", 0, "N/A", "N/A",
            List.of("N/A"), List.of("N/A"), ""
        );
    }
    
    /**
     * CountryData record - more complex than previous examples
     * 
     * Notice how we:
     * - Use List<String> for multiple values (languages, currencies)
     * - Include a helper method for formatting
     * - Handle image URLs for the flag
     */
    public record CountryData(
        String name,                    // "United States"
        String capital,                 // "Washington D.C."
        long population,                // 331449281
        String region,                  // "Americas"
        String subregion,               // "North America"
        List<String> currencies,        // ["United States dollar (USD)"]
        List<String> languages,         // ["English"]
        String flagUrl                  // "https://flagcdn.com/w320/us.png"
    ) {
        /**
         * Helper method to format population with commas
         * 331449281 becomes "331,449,281"
         */
        public String getFormattedPopulation() {
            return String.format("%,d", population);
        }
    }
}