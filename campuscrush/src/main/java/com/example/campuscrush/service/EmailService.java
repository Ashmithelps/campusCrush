package com.example.campuscrush.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EmailService {

    @Value("${mailersend.api-key}")
    private String apiKey;

    @Value("${mailersend.domain}")
    private String domain;

    @Value("${mailersend.from-email}")
    private String fromEmail;

    @Value("${mailersend.from-name}")
    private String fromName;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOtp(String to, String otp) {
        String url = "https://api.mailersend.com/v1/email";

        // JSON Body for MailerSend
        // They use a specific format:
        // { "from": { "email": "...", "name": "..." }, "to": [ { "email": "..." } ], "subject": "...", "text": "..." }
        
        // Constructing JSON manually to avoid creating extra DTO classes for one call
        String jsonBody = String.format(
            "{\"from\":{\"email\":\"%s\",\"name\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"CampusCrush OTP Code\",\"text\":\"Your authentication code is: %s\\n\\nThis code expires in 5 minutes.\",\"html\":\"<p>Your authentication code is: <strong>%s</strong></p><p>This code expires in 5 minutes.</p>\"}",
            fromEmail, fromName, to, otp, otp
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("✅ Email sent to " + to + " via MailerSend");
        } catch (Exception e) {
            System.err.println("❌ Failed to send email via MailerSend: " + e.getMessage());
            // Fallback to console for debugging
            logToConsole(to, otp);
        }
    }

    private void logToConsole(String to, String otp) {
        System.out.println("\n============ MOCK MAIL SERVICE ============");
        System.out.println("To: " + to);
        System.out.println("OTP: " + otp);
        System.out.println("===========================================\n");
    }
}
