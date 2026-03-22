package com.example.customerservice.config;

import jakarta.annotation.PreDestroy;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {
    private static final Logger logger = LoggerFactory.getLogger(Neo4jConfig.class);

    @Value("${neo4j.uri}")
    private String neo4jUri;

    @Value("${neo4j.username}")
    private String username;

    @Value("${neo4j.password}")
    private String password;

    @Value("${neo4j.database:neo4j}")
    private String database;

    private Driver driver;

    @Bean
    public Driver neo4jDriver() {
        logger.info("Initializing Neo4j driver: {}", neo4jUri);
        this.driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(username, password));
        return this.driver;
    }

    public void close() {
        if (driver != null) {
            driver.close();
        }
    }

    @PreDestroy
    public void destroy() {
        close();
    }
}