package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private Long id;

    // =========================
    // JOB INFO
    // =========================
    private Long jobId;
    private String jobTitle;
    private String jobCategory;
    private String jobLocation;
    private Long jobSalary;
    private String employmentType;
    private LocalDate jobExpiredAt;
    private String jobStatus;

    // =========================
    // COMPANY INFO
    // =========================
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private String companyAddress;

    // =========================
    // CANDIDATE SNAPSHOT
    // =========================
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateAddress;

    private String candidateSkills;
    private String candidateDesiredPosition;
    private String candidateDesiredCategory;
    private Integer candidateExperienceYears;
    private Long candidateExpectedSalary;
    private String candidateWorkType;
    private String candidateEducationLevel;
    private String candidateEnglishLevel;
    private String candidateCertificates;
    private String candidateProjects;
    private String candidateAvailableFrom;
    private String candidatePortfolioUrl;
    private String candidateLinkedinUrl;

    // =========================
    // APPLICATION INFO
    // =========================
    private String cvUrl;
    private String coverLetter;

    // PENDING / REVIEWED / ACCEPTED / REJECTED
    private String status;

    private LocalDateTime appliedAt;
}