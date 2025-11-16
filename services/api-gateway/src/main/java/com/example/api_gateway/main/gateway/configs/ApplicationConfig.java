package com.example.api_gateway.main.gateway.configs;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean
    public HttpMessageConverters customConverters() {
        return new HttpMessageConverters();
    }
}
