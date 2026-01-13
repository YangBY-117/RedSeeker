package com.redseeker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    
    // Allow all origins (development environment)
    // Production environment should specify the exact frontend address
    config.addAllowedOriginPattern("*");
    
    // Allowed HTTP methods
    config.addAllowedMethod("GET");
    config.addAllowedMethod("POST");
    config.addAllowedMethod("PUT");
    config.addAllowedMethod("DELETE");
    config.addAllowedMethod("OPTIONS");
    config.addAllowedMethod("PATCH");
    
    // Allowed request headers
    config.addAllowedHeader("*");
    
    // Allow credentials (e.g., Cookie, Authorization)
    config.setAllowCredentials(true);
    
    // Preflight request cache time (seconds)
    config.setMaxAge(3600L);
    
    // Apply CORS configuration to all paths
    source.registerCorsConfiguration("/**", config);
    
    return new CorsFilter(source);
  }
}
