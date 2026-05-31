package com.example.campuscrush.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /* =========================
       OTP EMAIL
       ========================= */
    public void sendOtp(String to, String otp) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(to);
            msg.setSubject("Your CampusCrush login code");
            msg.setText("Your login code is: " + otp + "\n\nThis code expires in 5 minutes.\n\n— CampusCrush");
            mailSender.send(msg);
            System.out.println("✅ OTP sent to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send OTP email: " + e.getMessage());
            logToConsole(to, otp);
        }
    }

    /* =========================
       CONFESSION NOTIFICATION
       ========================= */
    public void sendConfessionNotification(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, "CampusCrush"));
            helper.setTo(toEmail);
            helper.setSubject("Someone has a crush on you 💌");

            String text = "Someone on your campus sent you an anonymous confession on CampusCrush. "
                        + "Open the app to read it and decide whether to accept or decline.";

            String html = "<div style='font-family:sans-serif;max-width:480px;margin:0 auto;"
                        + "padding:40px 28px;background:#121212;border-radius:12px'>"
                        + "<p style='font-size:20px;font-weight:800;color:#FF2D55;margin:0 0 24px;"
                        + "letter-spacing:-0.5px'>campuscrush</p>"
                        + "<h2 style='font-size:22px;font-weight:700;color:#ffffff;margin:0 0 14px'>"
                        + "You have a secret admirer &#128140;</h2>"
                        + "<p style='color:#A7A7A7;font-size:15px;line-height:1.65;margin:0 0 28px'>"
                        + "Someone on your campus sent you an anonymous confession. "
                        + "Open the app to read it and decide whether to accept or decline.</p>"
                        + "<p style='color:#4A4A4A;font-size:12px;margin:0'>"
                        + "Their identity stays hidden until they choose to reveal it.</p>"
                        + "</div>";

            helper.setText(text, html);
            mailSender.send(message);
            System.out.println("✅ Confession notification sent to " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send confession notification: " + e.getMessage());
        }
    }

    private void logToConsole(String to, String otp) {
        System.out.println("\n============ MAIL FALLBACK ============");
        System.out.println("To:  " + to);
        System.out.println("OTP: " + otp);
        System.out.println("=======================================\n");
    }
}
