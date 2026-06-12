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

    private static final String SHELL_OPEN =
        "<div style='font-family:Georgia,serif;max-width:480px;margin:0 auto;"
        + "background:#1C1814;border-radius:12px;padding:48px 36px;'>"
        + "<p style='font-family:Georgia,serif;font-size:22px;color:#C4622D;"
        + "margin:0 0 36px;letter-spacing:0.06em;font-weight:600;'>Unsaid</p>";

    private static final String FOOTER =
        "<hr style='border:none;border-top:1px solid #2E2A26;margin:32px 0 20px;'/>"
        + "<p style='font-family:system-ui,Arial,sans-serif;font-size:11px;color:#4A4542;"
        + "line-height:1.5;margin:0;'>"
        + "You&#39;re receiving this because you&#39;re a student at CU Chandigarh. &middot; Unsaid</p>"
        + "</div>";

    /* =========================
       OTP EMAIL
       ========================= */
    public void sendOtp(String to, String otp) {
        String html = SHELL_OPEN
            + "<h2 style='font-family:Georgia,serif;font-size:20px;font-weight:600;"
            + "color:#F4EFE6;margin:0 0 24px;line-height:1.4;'>Your sign-in code</h2>"
            + "<p style='font-family:monospace,Courier New,monospace;font-size:30px;"
            + "letter-spacing:0.25em;color:#F4EFE6;background:#2E2A26;padding:20px 24px;"
            + "border-radius:8px;text-align:center;margin:0 0 16px;'>" + otp + "</p>"
            + "<p style='font-family:system-ui,Arial,sans-serif;font-size:13px;"
            + "color:#4A4542;margin:0;'>"
            + "Expires in 5 minutes. If you didn&#39;t request this, you can ignore it.</p>"
            + FOOTER;

        String body = "{\"sender\":{\"name\":\"Unsaid\",\"email\":\"" + fromEmail + "\"},"
            + "\"to\":[{\"email\":\"" + to + "\"}],"
            + "\"subject\":\"Your Unsaid sign-in code\","
            + "\"textContent\":\"Your sign-in code is: " + otp + "\\n\\nExpires in 5 minutes.\\n\\n— Unsaid\","
            + "\"htmlContent\":\"" + html.replace("\"", "\\\"") + "\"}";

        try {
            restTemplate.postForEntity(BREVO_URL, buildRequest(body), String.class);
        } catch (Exception e) {
            System.err.println("Failed to send OTP: " + e.getMessage());
        }
    }

    /* =========================
       CONFESSION NOTIFICATION
       ========================= */
    public void sendConfessionNotification(String toEmail) {
        String text = "Someone at your campus has something to say — anonymously. "
                    + "Open Unsaid to read it. You can accept or let it pass.";

        String html = SHELL_OPEN
            + "<h2 style='font-family:Georgia,serif;font-size:20px;font-weight:600;"
            + "color:#F4EFE6;margin:0 0 16px;line-height:1.4;'>"
            + "Someone left something unsaid for you.</h2>"
            + "<p style='font-family:system-ui,Arial,sans-serif;font-size:15px;"
            + "color:#A99B8E;line-height:1.7;margin:0 0 28px;'>" + text + "</p>"
            + "<p style='font-family:system-ui,Arial,sans-serif;font-size:12px;"
            + "color:#4A4542;margin:0;'>"
            + "Their identity stays hidden until they choose to reveal it.</p>"
            + FOOTER;

        String body = "{\"sender\":{\"name\":\"Unsaid\",\"email\":\"" + fromEmail + "\"},"
            + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
            + "\"subject\":\"Someone left something unsaid for you.\","
            + "\"textContent\":\"" + text + "\","
            + "\"htmlContent\":\"" + html.replace("\"", "\\\"") + "\"}";

        try {
            restTemplate.postForEntity(BREVO_URL, buildRequest(body), String.class);
        } catch (Exception e) {
            System.err.println("Failed to send confession notification: " + e.getMessage());
        }
    }

    /* =========================
       INVITE EMAIL
       ========================= */
    public void sendInvite(String toEmail) {
        String text = "Someone at CU Chandigarh left you an anonymous message — "
                    + "but you’re not on Unsaid yet, so it’s waiting. "
                    + "Step in to find out what was left unsaid: https://say-the-unsaid.vercel.app";

        String html = SHELL_OPEN
            + "<h2 style='font-family:Georgia,serif;font-size:20px;font-weight:600;"
            + "color:#F4EFE6;margin:0 0 16px;line-height:1.4;'>"
            + "Someone on your campus has something to say.</h2>"
            + "<p style='font-family:system-ui,Arial,sans-serif;font-size:15px;"
            + "color:#A99B8E;line-height:1.7;margin:0 0 28px;'>"
            + "Someone at CU Chandigarh left you an anonymous message &#8212; "
            + "but you&#39;re not on Unsaid yet, so it&#39;s waiting. "
            + "Step in to find out what was left unsaid.</p>"
            + "<a href='https://say-the-unsaid.vercel.app' "
            + "style='display:inline-block;background:#C4622D;color:#F4EFE6;"
            + "font-family:system-ui,Arial,sans-serif;font-size:15px;font-weight:600;"
            + "padding:14px 28px;border-radius:100px;text-decoration:none;margin-bottom:28px;'>"
            + "Step in</a>"
            + "<p style='font-family:system-ui,Arial,sans-serif;font-size:12px;"
            + "color:#4A4542;margin:0;'>"
            + "Their identity stays hidden until they choose to reveal it.</p>"
            + FOOTER;

        String body = "{\"sender\":{\"name\":\"Unsaid\",\"email\":\"" + fromEmail + "\"},"
            + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
            + "\"subject\":\"Someone on your campus has something to say.\","
            + "\"textContent\":\"" + text + "\","
            + "\"htmlContent\":\"" + html.replace("\"", "\\\"") + "\"}";

        try {
            restTemplate.postForEntity(BREVO_URL, buildRequest(body), String.class);
        } catch (Exception e) {
            System.err.println("Failed to send invite: " + e.getMessage());
        }
    }

    private HttpEntity<String> buildRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

}
