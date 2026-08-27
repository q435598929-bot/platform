package com.platform.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5174", "http://localhost:5175", "http://localhost:5176",
                        "http://localhost:9091", "http://127.0.0.1:9091",
                        "http://localhost:9092", "http://127.0.0.1:9092",
                        "http://localhost:9093", "http://127.0.0.1:9093")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
