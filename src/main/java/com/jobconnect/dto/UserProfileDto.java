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
    private String role;
    private String phone;
    private String address;
    private String bio;
    private String skills;
    private String cvUrl;
    private String avatarUrl;

    private String desiredPosition;
    private String desiredCategory;
    private Integer experienceYears;
    private Long expectedSalary;
    private String workType;
    private String educationLevel;
    private String englishLevel;
    private String certificates;
    private String projects;
    private String availableFrom;
}