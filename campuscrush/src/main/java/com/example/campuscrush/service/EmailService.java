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

    public void sendConfessionNotification(String toEmail) {
        String url = "https://api.mailersend.com/v1/email";

        String text = "Someone on your campus sent you an anonymous confession on CampusCrush. Open the app to read it and decide whether to accept or decline.";

        String html = "<div style=\"font-family:sans-serif;max-width:480px;margin:0 auto;padding:40px 28px;background:#121212;border-radius:12px\">"
            + "<p style=\"font-size:20px;font-weight:800;color:#FF2D55;margin:0 0 24px;letter-spacing:-0.5px\">campuscrush</p>"
            + "<h2 style=\"font-size:22px;font-weight:700;color:#ffffff;margin:0 0 14px\">You have a secret admirer &#128140;</h2>"
            + "<p style=\"color:#A7A7A7;font-size:15px;line-height:1.65;margin:0 0 28px\">"
            + "Someone on your campus sent you an anonymous confession. Open the app to read it and decide whether to accept or decline.</p>"
            + "<p style=\"color:#4A4A4A;font-size:12px;margin:0\">Their identity stays hidden until they choose to reveal it.</p>"
            + "</div>";

        String jsonBody = "{\"from\":{\"email\":\"" + fromEmail + "\",\"name\":\"" + fromName + "\"},"
            + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
            + "\"subject\":\"Someone has a crush on you 💌\","
            + "\"text\":\"" + text + "\","
            + "\"html\":\"" + html.replace("\"", "\\\"") + "\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(jsonBody, headers), String.class);
            System.out.println("✅ Confession notification sent to " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send confession notification: " + e.getMessage());
        }
    }

    private void logToConsole(String to, String otp) {
        System.out.println("\n============ MOCK MAIL SERVICE ============");
        System.out.println("To: " + to);
        System.out.println("OTP: " + otp);
        System.out.println("===========================================\n");
    }
}
