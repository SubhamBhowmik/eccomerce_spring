package com.example.eccomerce.service;


import com.example.eccomerce.event.NotificationEvent;
import com.example.eccomerce.event.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    // ─── ORDER EVENTS ──────────────────────────────────

    public void orderPlaced(String userId, String email,
                            String username, String orderId,
                            double total, Object items) {
        publish("order-events", userId, NotificationEvent.builder()
                .type(NotificationType.ORDER_PLACED)
                .userId(userId).userEmail(email).username(username)
                .priority("HIGH")
                .data(Map.of(
                        "orderId", orderId,
                        "total", total,
                        "items", items
                ))
                .build());
    }

    public void orderShipped(String userId, String email,
                             String username, String orderId,
                             String trackingId) {
        publish("order-events", userId, NotificationEvent.builder()
                .type(NotificationType.ORDER_SHIPPED)
                .userId(userId).userEmail(email).username(username)
                .priority("HIGH")
                .data(Map.of(
                        "orderId", orderId,
                        "trackingId", trackingId
                ))
                .build());
    }

    public void orderDelivered(String userId, String email,
                               String username, String orderId) {
        publish("order-events", userId, NotificationEvent.builder()
                .type(NotificationType.ORDER_DELIVERED)
                .userId(userId).userEmail(email).username(username)
                .priority("NORMAL")
                .data(Map.of("orderId", orderId))
                .build());
    }

    public void orderCancelled(String userId, String email,
                               String username, String orderId,
                               String reason) {
        publish("order-events", userId, NotificationEvent.builder()
                .type(NotificationType.ORDER_CANCELLED)
                .userId(userId).userEmail(email).username(username)
                .priority("HIGH")
                .data(Map.of("orderId", orderId, "reason", reason))
                .build());
    }

    // ─── AUTH EVENTS ────────────────────────────────────

    public void welcomeEmail(String userId, String email,
                             String username) {
        publish("auth-events", userId, NotificationEvent.builder()
                .type(NotificationType.WELCOME)
                .userId(userId).userEmail(email).username(username)
                .priority("NORMAL")
                .data(Map.of())
                .build());
    }

    public void passwordReset(String email, String resetLink) {
        publish("auth-events", email, NotificationEvent.builder()
                .type(NotificationType.PASSWORD_RESET)
                .userEmail(email)
                .priority("HIGH")
                .data(Map.of("resetLink", resetLink))
                .build());
    }

    // ─── OFFER EVENTS ────────────────────────────────────

    public void bigOffer(String userId, String email,
                         String username, String offerTitle,
                         String discount, String expiresAt) {
        publish("offer-events", userId, NotificationEvent.builder()
                .type(NotificationType.BIG_OFFER)
                .userId(userId).userEmail(email).username(username)
                .priority("HIGH")
                .data(Map.of(
                        "offerTitle", offerTitle,
                        "discount", discount,
                        "expiresAt", expiresAt
                ))
                .build());
    }

    // ─── CAMPAIGN (BULK) ─────────────────────────────────

    public void sendCampaign(String campaignId, String subject,
                             String htmlContent, String segment,
                             String email, String username) {
        publish("campaign-events", campaignId, NotificationEvent.builder()
                .type(NotificationType.CAMPAIGN_BLAST)
                .userEmail(email).username(username)
                .isBulk(true)
                .targetSegment(segment)
                .priority("LOW")
                .data(Map.of(
                        "campaignId", campaignId,
                        "subject", subject,
                        "htmlContent", htmlContent
                ))
                .build());
    }

    // ─── CORE PUBLISH ─────────────────────────────────────

    private void publish(String topic, String key,
                         NotificationEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish: {} | {}",
                                event.getType(), ex.getMessage());
                    } else {
                        log.info("Published: {} → topic: {} | partition: {} | offset: {}",
                                event.getType(), topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    // -------------- for OTP
    public void sendOtpEmail(String email, String otp) {
        publish("auth-events", email, NotificationEvent.builder()
                .type(NotificationType.OTP)
                .userEmail(email)
                .priority("HIGH")
                .data(Map.of("otp", otp))
                .build());
    }
}