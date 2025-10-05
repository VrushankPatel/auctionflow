package com.auctionflow.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.hibernate.jpa.HibernatePersistenceProvider;

@Configuration
@Profile("!ui-only")
@EntityScan(basePackages = {"com.auctionflow.api.entities", "com.auctionflow.events.persistence", "com.auctionflow.common"})
@EnableJpaRepositories(basePackages = {"com.auctionflow.api.repositories", "com.auctionflow.events.persistence"}, entityManagerFactoryRef = "entityManagerFactory")
@EnableTransactionManagement
public class JpaConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.auctionflow.api.entities", "com.auctionflow.events.persistence", "com.auctionflow.common");
        // Set JPA properties
        java.util.Map<String, Object> jpaProperties = new java.util.HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", environment.getProperty("spring.jpa.hibernate.ddl-auto", "none"));
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        em.setJpaPropertyMap(jpaProperties);
        em.setPersistenceProvider(new HibernatePersistenceProvider());
        return em;
    }

    @Bean
    @Primary
    public JpaTransactionManager transactionManager(@Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    @Bean(name = "entityManager")
    public EntityManager entityManager(@Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory.getObject());
    }

}