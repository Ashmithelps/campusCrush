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
        String normalizedEmail = email.toLowerCase().trim();
        if (!normalizedEmail.endsWith("@cuchd.in")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Only @cuchd.in emails are allowed"
            );
        }

        User user = userRepository.findByCollegeEmail(normalizedEmail).orElse(null);

        if (user == null) {
            String rollNumber = normalizedEmail.split("@")[0].toUpperCase();
            user = userRepository.findByRollNumber(rollNumber).orElse(null);

            if (user == null) {
                user = User.builder()
                        .collegeEmail(normalizedEmail)
                        .rollNumber(rollNumber)
                        .displayAlias(aliasGenerator.generate())
                        .build();
                userRepository.save(user);
            } else if (!user.getCollegeEmail().equals(normalizedEmail)) {
                user.setCollegeEmail(normalizedEmail);
                userRepository.save(user);
            }
        }

        authService.generateAndSendOtp(user);
        return "OTP sent to " + normalizedEmail;
    }

    @PostMapping("/login")
    public String login(@RequestParam String email) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByCollegeEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No account found for this email. Please register first."));

        authService.generateAndSendOtp(user);
        return "OTP sent to " + normalizedEmail;
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return authService.verifyOtpAndLogin(email, otp);
    }
}