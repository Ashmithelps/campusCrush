package com.example.campuscrush.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:console}")
    private String fromEmail;

    public void sendOtp(String to, String otp) {
        String subject = "CampusCrush OTP Code";
        String body = "Your authentication code is: " + otp + "\n\nThis code expires in 5 minutes.";

        if (mailSender != null && !fromEmail.equals("console")) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                System.out.println("✅ Email sent to " + to);
            } catch (Exception e) {
                System.err.println("❌ Failed to send email: " + e.getMessage());
                // Fallback to console
                logToConsole(to, otp);
            }
        } else {
            // Mock mode
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
