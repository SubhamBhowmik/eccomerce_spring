package com.example.eccomerce.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private String eventId;
    private NotificationType type;
    private String userId;
    private String userEmail;
    private String username;
    private boolean isBulk;
    private String targetSegment;
    private Map<String, Object> data;
    private String priority;
    private LocalDateTime timestamp;
}
