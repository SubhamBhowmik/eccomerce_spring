package com.example.eccomerce.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MongoKeepAlive {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Scheduled(fixedDelay = 86400000) // every 24 hours
    public void keepAlive() {
        mongoTemplate.executeCommand("{ ping: 1 }");
        System.out.println("MongoDB pinged at: " + java.time.LocalDateTime.now());
    }
}