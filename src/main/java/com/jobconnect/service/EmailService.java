package com.jobconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject,String content) {
        try {
            SimpleMailMessage message =new SimpleMailMessage();

            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);

            log.info("Email sent successfully to: {}", to );
        } catch (MailException e) {

            log.error("Failed to send email to: {}", to, e );

            throw new RuntimeException(
                    "Không thể gửi email"
            );
        }
    }
}