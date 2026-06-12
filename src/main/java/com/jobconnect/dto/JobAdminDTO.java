package com.jobconnect.dto;

import com.jobconnect.entity.EmploymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAdminDTO {

    private Long id;

    private String title;

    private String location;

    private Long salary;

    private String category;

    private String description;

    private String requirements;

    private EmploymentType employmentType;

    private LocalDateTime createdAt;

    private LocalDate expiredAt;

    private String status;

    private String rejectionReason;

    private Long companyId;

    private String companyName;

    private String companyLogo;

    private String companyAddress;
}