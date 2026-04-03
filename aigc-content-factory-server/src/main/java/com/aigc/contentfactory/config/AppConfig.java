package com.aigc.contentfactory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer(AppProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(properties.getCors().getAllowedOrigin())
                        .allowedMethods("*");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                Path generatedRoot = Path.of(properties.getStorage().getRootDir()).toAbsolutePath().normalize();
                registry.addResourceHandler(properties.getStorage().getBaseUrl() + "/**")
                        .addResourceLocations(generatedRoot.toUri().toString());
            }
        };
    }

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.modules(new JavaTimeModule()).build();
    }
}
