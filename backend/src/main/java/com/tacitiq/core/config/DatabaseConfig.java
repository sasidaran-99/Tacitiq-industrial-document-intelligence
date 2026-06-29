package com.tacitiq.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.neo4j.driver.Driver;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;

@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.tacitiq.modules.auth.repository",
        "com.tacitiq.modules.asset.repository",
        "com.tacitiq.modules.document.repository",
        "com.tacitiq.modules.incident.repository",
        "com.tacitiq.modules.compliance.repository",
        "com.tacitiq.modules.maintenance.repository",
        "com.tacitiq.modules.knowledge.repository",
        "com.tacitiq.modules.ai.repository"
    },
    transactionManagerRef = "transactionManager"
)
@EnableNeo4jRepositories(
    basePackages = {
        "com.tacitiq.modules.graph.repository"
    },
    transactionManagerRef = "neo4jTransactionManager"
)
public class DatabaseConfig {

    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(
            Driver driver,
            DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}
