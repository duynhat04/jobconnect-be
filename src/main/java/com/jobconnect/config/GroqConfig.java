package com.jobconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GroqConfig {

    @Bean
    public WebClient groqWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.groq.com/openai/v1")
                .build();
    }
}