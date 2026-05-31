package com.example.campuscrush.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EmailService {

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.from-email}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    /* =========================
       OTP EMAIL
       ========================= */
    public void sendOtp(String to, String otp) {
        String body = "{\"sender\":{\"name\":\"CampusCrush\",\"email\":\"" + fromEmail + "\"},"
            + "\"to\":[{\"email\":\"" + to + "\"}],"
            + "\"subject\":\"Your CampusCrush login code\","
            + "\"textContent\":\"Your login code is: " + otp + "\\n\\nThis code expires in 5 minutes.\\n\\n— CampusCrush\"}";

        try {
            restTemplate.postForEntity(BREVO_URL, buildRequest(body), String.class);
            System.out.println("✅ OTP sent to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send OTP: " + e.getMessage());
            logToConsole(to, otp);
        }
    }

    /* =========================
       CONFESSION NOTIFICATION
       ========================= */
    public void sendConfessionNotification(String toEmail) {
        String text = "Someone on your campus sent you an anonymous confession on CampusCrush. "
                    + "Open the app to read it and decide whether to accept or decline.";

        String html = "<div style='font-family:sans-serif;max-width:480px;margin:0 auto;"
                    + "padding:40px 28px;background:#121212;border-radius:12px'>"
                    + "<p style='font-size:20px;font-weight:800;color:#FF2D55;margin:0 0 24px'>"
                    + "campuscrush</p>"
                    + "<h2 style='font-size:22px;font-weight:700;color:#ffffff;margin:0 0 14px'>"
                    + "You have a secret admirer &#128140;</h2>"
                    + "<p style='color:#A7A7A7;font-size:15px;line-height:1.65;margin:0 0 28px'>"
                    + text + "</p>"
                    + "<p style='color:#4A4A4A;font-size:12px;margin:0'>"
                    + "Their identity stays hidden until they choose to reveal it.</p>"
                    + "</div>";

        String body = "{\"sender\":{\"name\":\"CampusCrush\",\"email\":\"" + fromEmail + "\"},"
            + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
            + "\"subject\":\"Someone has a crush on you \\uD83D\\uDC8C\","
            + "\"textContent\":\"" + text + "\","
            + "\"htmlContent\":\"" + html.replace("\"", "\\\"") + "\"}";

        try {
            restTemplate.postForEntity(BREVO_URL, buildRequest(body), String.class);
            System.out.println("✅ Confession notification sent to " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send confession notification: " + e.getMessage());
        }
    }

    private HttpEntity<String> buildRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private void logToConsole(String to, String otp) {
        System.out.println("\n============ MAIL FALLBACK ============");
        System.out.println("To:  " + to);
        System.out.println("OTP: " + otp);
        System.out.println("=======================================\n");
    }
}
