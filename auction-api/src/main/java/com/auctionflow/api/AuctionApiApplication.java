package com.auctionflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class, org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableKafka
@ComponentScan(basePackages = {"com.auctionflow.api", "com.auctionflow.events", "com.auctionflow.common"})
public class AuctionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionApiApplication.class, args);
    }
}