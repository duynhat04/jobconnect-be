package com.jobconnect.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String confirmPassword; // Hứng thêm trường này để validate
}