package com.example.campuscrush.service;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.UserRepository;
import com.example.campuscrush.security.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_RATE_LIMIT_SECONDS = 60;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public void generateAndSendOtp(User user) {
        Instant now = Instant.now();

        if (user.getLastOtpSentAt() != null &&
                user.getLastOtpSentAt().plusSeconds(OTP_RATE_LIMIT_SECONDS).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Please wait before requesting another OTP");
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        user.setOtpCode(otp);
        user.setOtpExpiry(now.plusSeconds(300));
        user.setLastOtpSentAt(now);
        userRepository.save(user);

        emailService.sendOtp(user.getCollegeEmail(), otp);
    }

    public String verifyOtpAndLogin(String email, String otp) {
        String normalizedEmail = email.toLowerCase().trim();
        String normalizedOtp = otp.trim();

        User user = userRepository.findByCollegeEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Instant now = Instant.now();

        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Too many failed attempts. Try again in " + OTP_LOCKOUT_MINUTES + " minutes.");
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(normalizedOtp)) {
            int attempts = user.getOtpAttempts() + 1;
            user.setOtpAttempts(attempts);
            if (attempts >= OTP_MAX_ATTEMPTS) {
                user.setOtpLockedUntil(now.plusSeconds(OTP_LOCKOUT_MINUTES * 60L));
                user.setOtpAttempts(0);
                userRepository.save(user);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Account locked for " + OTP_LOCKOUT_MINUTES + " minutes.");
            }
            userRepository.save(user);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        if (user.getOtpExpiry().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP Expired");
        }

        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
        user.setOtpLockedUntil(null);
        user.setVerified(true);
        userRepository.save(user);

        return jwtUtil.generateToken(user.getPublicId());
    }
}
