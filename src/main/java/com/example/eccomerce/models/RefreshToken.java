package com.example.eccomerce.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id
    private String id;
    private String token;
    private String userId;
    private String email;
    private LocalDateTime expiryDate;
    private boolean revoked = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String deviceInfo; // browser/device info
}