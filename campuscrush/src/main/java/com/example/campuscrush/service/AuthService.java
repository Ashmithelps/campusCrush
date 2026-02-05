package com.example.campuscrush.service;

import java.time.Instant;
import java.util.Random;

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

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder; // Assuming you have one, or we can add it

    public void generateAndSendOtp(User user) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiry(Instant.now().plusSeconds(300)); // 5 minutes
        userRepository.save(user);

        emailService.sendOtp(user.getCollegeEmail(), otp);
    }

    public String verifyOtpAndLogin(String email, String otp) {
        User user = userRepository.findByCollegeEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        if (user.getOtpExpiry().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP Expired");
        }

        // OTP Valid
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setVerified(true);
        userRepository.save(user);

        return jwtUtil.generateToken(user.getPublicId());
    }
}
