package com.example.eccomerce.controller;

import com.example.eccomerce.config.JwtUtil;
import com.example.eccomerce.models.RefreshToken;
import com.example.eccomerce.models.User;
import com.example.eccomerce.repository.RefreshTokenRepository;
import com.example.eccomerce.repository.UserRepository;
import com.example.eccomerce.service.AuthService;
import com.example.eccomerce.service.NotificationEventPublisher;
import com.example.eccomerce.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationEventPublisher publisher;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        user.setRole("USER"); // always USER
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved =  userRepository.save(user);
        publisher.welcomeEmail(
                saved.getId(),
                saved.getEmail(),
                saved.getUsername()
        );
        return ResponseEntity.ok(Map.of("message", "Registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String deviceInfo = request.getHeader("User-Agent");
            Map<String, Object> response = authService.login(
                    body.get("email"),
                    body.get("password"),
                    deviceInfo
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            Map<String, String> tokens = authService.refresh(body.get("refreshToken"));
            return ResponseEntity.ok(tokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        authService.logout(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // Only YOU (ADMIN) can promote users
    @PostMapping("/make-admin")
    public ResponseEntity<?> makeAdmin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        User user = userRepository.findByEmail(email)
                .orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        user.setRole("ADMIN");
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", email + " is now ADMIN"));
    }

    // Disable/enable user account (ADMIN only)
    @PostMapping("/toggle-user")
    public ResponseEntity<?> toggleUser(@RequestBody Map<String, String> body) {
        User user = userRepository.findByEmail(body.get("email")).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User status: " + (user.isEnabled() ? "enabled" : "disabled")));
    }



    // ─── SEND OTP ──────────────────────────────────────────────────
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        // Check user exists
        if (!userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User not found"));
        }

        try {
            // Generate OTP and store in Redis
            String otp = otpService.generateAndStoreOtp(email);

            // Send OTP via email (Kafka → notification service)
            publisher.sendOtpEmail(email, otp);

            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to " + email,
                    "expiresIn", "5 minutes"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(429)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── VERIFY OTP ────────────────────────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email      = body.get("email");
        String enteredOtp = body.get("otp");

        try {
            // Verify OTP from Redis
            otpService.verifyOtp(email, enteredOtp);

            // OTP correct → issue JWT tokens
            User user = userRepository.findByEmail(email).get();
            String accessToken  = jwtUtil.generateAccessToken(
                    user.getEmail(), user.getRole(), user.getId()
            );
            String newRefreshToken = jwtUtil.generateRefreshToken(
                    user.getEmail(), user.getId()
            );

            // Save refresh token
            RefreshToken newStored = new RefreshToken();
            newStored.setToken(newRefreshToken);
            newStored.setUserId(user.getId());
            newStored.setEmail(user.getEmail());
            newStored.setExpiryDate(LocalDateTime.now().plusDays(7));
            refreshTokenRepository.save(newStored);

            return ResponseEntity.ok(Map.of(
                    "accessToken",  accessToken,
                    "refreshToken", newRefreshToken,
                    "role",         user.getRole(),
                    "username",     user.getUsername()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── RESET PASSWORD (Forgot Password) ──────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email       = body.get("email");
        String enteredOtp  = body.get("otp");
        String newPassword = body.get("newPassword");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (enteredOtp == null || enteredOtp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP is required"));
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }

        try {
            // Verify OTP from Redis (one-time use)
            otpService.verifyOtp(email, enteredOtp);

            // Find user and update password
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Revoke all existing refresh tokens for security
            refreshTokenRepository.deleteByUserId(user.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Password reset successfully. Please login with your new password."
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
