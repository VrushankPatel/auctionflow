package com.auctionflow.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuctionNotificationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionNotificationsApplication.class, args);
    }
}