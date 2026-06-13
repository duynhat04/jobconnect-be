package com.jobconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    // =========================
    // SEND COMMON EMAIL
    // Giữ method này để UserService hiện tại vẫn dùng được
    // =========================
    public void sendEmail(String to, String subject, String content) {
        String safeTo = normalize(to).toLowerCase();
        String safeSubject = normalize(subject);
        String safeContent = content == null ? "" : content.trim();

        validateEmailInput(safeTo, safeSubject, safeContent);

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            if (StringUtils.hasText(fromEmail)) {
                message.setFrom(fromEmail.trim());
                message.setReplyTo(fromEmail.trim());
            }

            message.setTo(safeTo);
            message.setSubject(safeSubject);
            message.setText(safeContent);

            mailSender.send(message);

            log.info("Gửi email thành công đến: {}", maskEmail(safeTo));
        } catch (MailAuthenticationException e) {
            log.error("Xác thực Gmail/App Password thất bại khi gửi đến: {}", maskEmail(safeTo), e);

            throw new RuntimeException(
                    "Không thể gửi email: Gmail/App Password không hợp lệ!"
            );
        } catch (MailSendException e) {
            log.error("SMTP không gửi được email đến: {}", maskEmail(safeTo), e);

            throw new RuntimeException(
                    "Không thể gửi email: máy chủ SMTP đang lỗi hoặc bị từ chối!"
            );
        } catch (MailException e) {
            log.error("Gửi email thất bại đến: {}", maskEmail(safeTo), e);

            throw new RuntimeException(
                    "Không thể gửi email. Vui lòng kiểm tra cấu hình Gmail/App Password!"
            );
        }
    }

    // =========================
    // REGISTER OTP
    // =========================
    public void sendRegisterOtpEmail(String to, String fullName, String otp) {
        validateOtp(otp);

        String safeName = getSafeName(fullName);

        String content = """
                Xin chào %s,

                Cảm ơn bạn đã đăng ký tài khoản JobConnect.

                Mã OTP xác thực tài khoản của bạn là:
                %s

                Mã OTP có hiệu lực trong 5 phút.

                Nếu bạn không thực hiện đăng ký tài khoản, vui lòng bỏ qua email này.

                Trân trọng,
                JobConnect Team
                """.formatted(safeName, otp);

        sendEmail(
                to,
                "Mã xác thực tài khoản JobConnect",
                content
        );
    }

    // =========================
    // RESEND OTP
    // =========================
    public void sendResendOtpEmail(String to, String fullName, String otp) {
        validateOtp(otp);

        String safeName = getSafeName(fullName);

        String content = """
                Xin chào %s,

                Mã OTP mới để xác thực tài khoản JobConnect của bạn là:
                %s

                Mã OTP có hiệu lực trong 5 phút.

                Nếu bạn không yêu cầu gửi lại OTP, vui lòng bỏ qua email này.

                Trân trọng,
                JobConnect Team
                """.formatted(safeName, otp);

        sendEmail(
                to,
                "OTP mới xác thực JobConnect",
                content
        );
    }

    // =========================
    // RESET PASSWORD OTP
    // =========================
    public void sendResetPasswordOtpEmail(String to, String fullName, String otp) {
        validateOtp(otp);

        String safeName = getSafeName(fullName);

        String content = """
                Xin chào %s,

                Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản JobConnect.

                Mã OTP đặt lại mật khẩu của bạn là:
                %s

                Mã OTP có hiệu lực trong 5 phút.

                Nếu bạn không yêu cầu thao tác này, vui lòng bỏ qua email này.

                Trân trọng,
                JobConnect Team
                """.formatted(safeName, otp);

        sendEmail(
                to,
                "Mã OTP đặt lại mật khẩu JobConnect",
                content
        );
    }

    // =========================
    // VALIDATE
    // =========================
    private void validateEmailInput(String to, String subject, String content) {
        if (!StringUtils.hasText(to)) {
            throw new RuntimeException("Email người nhận không được để trống!");
        }

        if (!EMAIL_PATTERN.matcher(to).matches()) {
            throw new RuntimeException("Email người nhận không hợp lệ!");
        }

        if (!StringUtils.hasText(subject)) {
            throw new RuntimeException("Tiêu đề email không được để trống!");
        }

        if (subject.length() > 150) {
            throw new RuntimeException("Tiêu đề email quá dài!");
        }

        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("Nội dung email không được để trống!");
        }
    }

    private void validateOtp(String otp) {
        if (!StringUtils.hasText(otp)) {
            throw new RuntimeException("Mã OTP không được để trống!");
        }

        if (!otp.matches("^\\d{6}$")) {
            throw new RuntimeException("Mã OTP không hợp lệ!");
        }
    }

    private String getSafeName(String fullName) {
        return StringUtils.hasText(fullName)
                ? fullName.trim()
                : "bạn";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];

        if (name.length() <= 2) {
            return "***@" + domain;
        }

        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }
}