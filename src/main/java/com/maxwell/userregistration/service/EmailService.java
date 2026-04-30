package com.maxwell.userregistration.service;

import com.maxwell.userregistration.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.base-url}")
    private String baseUrl;

    @Async
    public void sendVerificationEmail(User user, String token) {
        String link = baseUrl + "/api/auth/verify-email?token=" + token;
        String body = String.format("""
                Hi %s,

                Please verify your email address by clicking the link below:

                %s

                This link expires in 24 hours.

                If you did not create this account, you can safely ignore this email.

                — User Registration System
                """, user.getFirstName(), link);

        send(user.getEmail(), "Verify your email address", body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.debug("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
