package com.test.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Global Jackson ObjectMapper configuration.
 * Configures JSON serialization/deserialization for the entire application.
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates and configures the primary ObjectMapper bean.
     * - Registers JavaTimeModule for Java 8 date/time support
     * - Disables writing dates as timestamps (uses ISO-8601 format instead)
     *
     * @return configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register module for Java 8 date/time types (Instant, LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());

        // Write dates as ISO-8601 strings instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    /**
     * Creates and configures XmlMapper bean for XML parsing.
     * Used for TestRail XML import functionality.
     *
     * @return configured XmlMapper instance
     */
    @Bean
    public XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();

        // Register module for Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());

        // Write dates as ISO-8601 strings instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}

