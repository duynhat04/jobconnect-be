package com.jobconnect.controller;

import com.jobconnect.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class MailController {

    private final EmailService emailService;

    @GetMapping("/mail")
    public ResponseEntity<String> testMail() {

        emailService.sendEmail(
                "luannhatk4@gmail.com",
                "JobConnect SMTP Test",
                "SMTP hoạt động thành công"
        );

        return ResponseEntity.ok(
                "Đã gửi email thành công"
        );
    }
}