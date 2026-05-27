package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationCandidateDto {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;

    private Long candidateId;
    private String fullName;
    private String email;
    private String phone;
    private String address;

    private String bio;
    private String skills;
    private String cvUrl;
    private String coverLetter;

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

    private String status;
    private LocalDateTime appliedAt;

    private Integer matchScore;
}