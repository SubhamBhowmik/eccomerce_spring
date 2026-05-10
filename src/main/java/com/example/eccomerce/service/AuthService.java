package com.example.eccomerce.service;
import com.example.eccomerce.config.JwtUtil;
import com.example.eccomerce.models.RefreshToken;
import com.example.eccomerce.models.User;
import com.example.eccomerce.repository.RefreshTokenRepository;
import com.example.eccomerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(String email, String password, String deviceInfo) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isEnabled()) throw new RuntimeException("Account disabled");

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Delete old refresh tokens for this user
        refreshTokenRepository.deleteByUserId(user.getId());

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getId());
        String refreshTokenStr = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        // Save refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setUserId(user.getId());
        refreshToken.setEmail(user.getEmail());
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setDeviceInfo(deviceInfo);
        refreshTokenRepository.save(refreshToken);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshTokenStr,
                "role", user.getRole(),
                "username", user.getUsername(),
                "userId", user.getId()
        );
    }

    public Map<String, String> refresh(String refreshTokenStr) {
        // Find token in DB
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // Check if revoked
        if (storedToken.isRevoked()) throw new RuntimeException("Refresh token revoked");

        // Check expiry
        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("Refresh token expired, please login again");
        }

        // Validate JWT signature
        jwtUtil.isValid(refreshTokenStr);

        // Get user
        User user = userRepository.findByEmail(storedToken.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Rotate refresh token (delete old, create new)
        refreshTokenRepository.delete(storedToken);

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getId());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        RefreshToken newStored = new RefreshToken();
        newStored.setToken(newRefreshToken);
        newStored.setUserId(user.getId());
        newStored.setEmail(user.getEmail());
        newStored.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(newStored);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(refreshTokenRepository::delete);
    }
}
