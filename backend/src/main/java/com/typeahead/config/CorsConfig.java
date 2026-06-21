package com.typeahead.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// (1) Marks this as a Spring configuration class
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // (2) Override the CORS mapping method from WebMvcConfigurer
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                // (3) Apply this CORS policy to ALL endpoints
                .addMapping("/**")
                // (4) Allow requests from React dev server and production frontend
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173", // Vite default port
                        "http://frontend:3000" // Docker service name
                )
                // (5) Allow these HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // (6) Allow all headers
                .allowedHeaders("*")
                // (7) Allow cookies/auth headers if needed
                .allowCredentials(true);
    }
}