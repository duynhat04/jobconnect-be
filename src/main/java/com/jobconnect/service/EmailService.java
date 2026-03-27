package com.jobconnect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();

        // Cấu hình nội dung thư
        message.setFrom("JobConnect System <dia_chi_email_cua_ban@gmail.com>"); // Thay email của bạn vào đây
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        // Bấm nút gửi!
        mailSender.send(message);
    }
}