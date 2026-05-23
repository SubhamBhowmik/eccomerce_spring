package com.example.eccomerce.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    @Value("${otp.max.attempts}")
    private int maxAttempts;

    @Value("${otp.resend.cooldown.seconds}")
    private int resendCooldown;

    private final SecureRandom secureRandom = new SecureRandom();

    // Redis key patterns
    private static final String OTP_KEY      = "otp:%s";           // otp:email
    private static final String ATTEMPT_KEY  = "otp:attempts:%s";  // otp:attempts:email
    private static final String COOLDOWN_KEY = "otp:cooldown:%s";  // otp:cooldown:email
    private static final String BLOCKED_KEY  = "otp:blocked:%s";   // otp:blocked:email

    // ─── GENERATE OTP ──────────────────────────────────────────

    public String generateAndStoreOtp(String email) {

        // 1. Check if user is blocked
        String blockedKey = String.format(BLOCKED_KEY, email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockedKey))) {
            Long ttl = redisTemplate.getExpire(blockedKey, TimeUnit.SECONDS);
            throw new RuntimeException(
                    "Too many attempts. Try again in " + ttl + " seconds"
            );
        }

        // 2. Check resend cooldown
        String cooldownKey = String.format(COOLDOWN_KEY, email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            throw new RuntimeException(
                    "Please wait " + ttl + " seconds before resending"
            );
        }

        // 3. Generate 6 digit OTP
        String otp = String.valueOf(
                100000 + secureRandom.nextInt(900000)
        );

        // 4. Store OTP in Redis with TTL
        String otpKey = String.format(OTP_KEY, email);
        redisTemplate.opsForValue().set(
                otpKey,
                otp,
                otpExpiryMinutes,
                TimeUnit.MINUTES   // ← auto expires in 5 mins ✅
        );

        // 5. Reset attempt counter
        String attemptKey = String.format(ATTEMPT_KEY, email);
        redisTemplate.opsForValue().set(
                attemptKey,
                "0",
                otpExpiryMinutes,
                TimeUnit.MINUTES
        );

        // 6. Set resend cooldown
        redisTemplate.opsForValue().set(
                cooldownKey,
                "1",
                resendCooldown,
                TimeUnit.SECONDS   // ← can't resend for 30 seconds
        );

        // Never log OTP in production!
        log.info("OTP generated for: {}", email);

        return otp; // return to send via email
    }

    // ─── VERIFY OTP ────────────────────────────────────────────

    public boolean verifyOtp(String email, String enteredOtp) {

        String otpKey     = String.format(OTP_KEY, email);
        String attemptKey = String.format(ATTEMPT_KEY, email);
        String blockedKey = String.format(BLOCKED_KEY, email);

        // 1. Check if OTP exists (not expired)
        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) {
            throw new RuntimeException("OTP expired or not found. Please resend.");
        }

        // 2. Check attempt count
        String attempts = redisTemplate.opsForValue().get(attemptKey);
        int attemptCount = attempts != null ? Integer.parseInt(attempts) : 0;

        if (attemptCount >= maxAttempts) {
            // Block user for 1 hour
            redisTemplate.opsForValue().set(
                    blockedKey, "1", 1, TimeUnit.HOURS
            );
            // Delete OTP
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptKey);

            throw new RuntimeException(
                    "Too many wrong attempts. Blocked for 1 hour."
            );
        }

        // 3. Verify OTP
        if (!storedOtp.equals(enteredOtp)) {
            // Increment attempt counter
            redisTemplate.opsForValue().increment(attemptKey);

            int remaining = maxAttempts - attemptCount - 1;
            throw new RuntimeException(
                    "Wrong OTP. " + remaining + " attempts remaining."
            );
        }

        // 4. OTP correct! Delete immediately (one-time use)
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptKey);
        redisTemplate.delete(String.format(COOLDOWN_KEY, email));

        log.info("OTP verified successfully for: {}", email);
        return true;
    }

    // ─── CHECK STATUS ───────────────────────────────────────────

    public long getOtpTtl(String email) {
        String otpKey = String.format(OTP_KEY, email);
        Long ttl = redisTemplate.getExpire(otpKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }
}