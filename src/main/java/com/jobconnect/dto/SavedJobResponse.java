package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedJobResponse {

    private Long id;

    private Long jobId;

    private String jobTitle;

    private String jobLocation;

    private Long jobSalary;

    private String jobCategory;

    private String employmentType;

    private String jobStatus;

    private Long companyId;

    private String companyName;

    private String companyLogo;

    private LocalDateTime savedAt;
}