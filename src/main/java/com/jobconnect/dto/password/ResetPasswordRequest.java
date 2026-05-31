package com.jobconnect.dto.password;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String otp;
    private String newPassword;
    private String confirmPassword;
}