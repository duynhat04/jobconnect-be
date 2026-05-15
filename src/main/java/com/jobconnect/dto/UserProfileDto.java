package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
    private String email; 
    private String fullName;
    private String phone;
    private String address;
    private String bio;
    private String skills; 
    private String cvUrl;
}