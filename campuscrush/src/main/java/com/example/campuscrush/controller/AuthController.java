package com.example.campuscrush.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.alias.AliasGenerator;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.UserRepository;
import com.example.campuscrush.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final AliasGenerator aliasGenerator;
    private final AuthService authService;

    @PostMapping("/register")
    public String register(@RequestParam String email) {
        try {
            // 1. Normalize & Enforce Domain
            String normalizedEmail = email.toLowerCase().trim();
            if (!normalizedEmail.endsWith("@cuchd.in")) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only @cuchd.in emails Allowed"
                );
            }

            User user = userRepository.findByCollegeEmail(normalizedEmail).orElse(null);

            if (user == null) {
                // Check if user exists by Roll Number (to avoid duplicates if email case differed previously)
                String[] parts = normalizedEmail.split("@");
                String rollNumber = parts[0].toUpperCase();
                
                user = userRepository.findByRollNumber(rollNumber).orElse(null);

                if (user == null) {
                    // Create new user (unverified initially)
                    user = User.builder()
                            .collegeEmail(normalizedEmail)
                            .rollNumber(rollNumber)
                            .displayAlias(aliasGenerator.generate())
                            .build();
                    userRepository.save(user);
                } else {
                    // Update existing user with normalized email if needed
                    if (!user.getCollegeEmail().equals(normalizedEmail)) {
                        user.setCollegeEmail(normalizedEmail);
                        userRepository.save(user);
                    }
                }
            }

            // 2. Send OTP
            authService.generateAndSendOtp(user);

            return "OTP sent to " + normalizedEmail;

        } catch (Exception e) {
            e.printStackTrace(); // Print full error to console
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            User user = userRepository.findByCollegeEmail(normalizedEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // Send OTP
            authService.generateAndSendOtp(user);
            
            return "OTP sent to " + normalizedEmail;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return authService.verifyOtpAndLogin(email, otp);
    }
}