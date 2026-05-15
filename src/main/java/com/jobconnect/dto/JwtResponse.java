package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor 
public class JwtResponse {
    private String token;
    private String refreshToken;
    private Long id;
    private String email;
    private String fullName;
    private String role;
}