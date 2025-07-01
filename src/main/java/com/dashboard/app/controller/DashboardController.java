package com.dashboard.app.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;

import com.dashboard.app.service.CountryService;
import com.dashboard.app.service.QuoteService;
import com.dashboard.app.service.WeatherService;

@Controller
public class DashboardController {
    
    @Autowired
    private WeatherService weatherService;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private CountryService countryService;

    public DashboardController(WeatherService weatherService, 
                            QuoteService quoteService, 
                            CountryService countryService) {
        this.weatherService = weatherService;
        this.quoteService = quoteService;
        this.countryService = countryService;
    }
    @GetMapping("/")
    public String dashboard(
            @RequestParam(value = "city", defaultValue = "London") String city,
            @RequestParam(value = "country", defaultValue = "United Kingdom") String country,
            Model model) {
        
        model.addAttribute("weather", weatherService.getCurrentWeather(city));
        model.addAttribute("quote", quoteService.getRandomQuote());
        model.addAttribute("countryInfo", countryService.getCountryInfo(country));  // Add this
        
        model.addAttribute("currentCity", city);
        model.addAttribute("currentCountry", country);  // For form defaults
        
        return "dashboard";
    }
}