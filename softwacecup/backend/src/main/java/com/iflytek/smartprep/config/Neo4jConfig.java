package com.iflytek.smartprep.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    @Value("${smartprep.neo4j.uri:bolt://localhost:7687}")
    private String uri;

    @Value("${smartprep.neo4j.username:neo4j}")
    private String username;

    @Value("${smartprep.neo4j.password:your_neo4j_password}")
    private String password;

    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
}
