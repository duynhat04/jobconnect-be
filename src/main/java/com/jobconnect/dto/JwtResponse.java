package com.jobconnect.dto;

import com.jobconnect.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor // Tự tạo Constructor có tham số
public class JwtResponse {
    private String token;
    private User user;
}