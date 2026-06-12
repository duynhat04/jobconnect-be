package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponse {

    private Long id;

    private String name;

    private String taxCode;

    private String address;

    private String description;

    private String logo;

    private String website;

    private String phone;

    private String status;

    private Integer remainingPosts;

    private LocalDateTime createdAt;

    private Long userId;

    private String userEmail;

    private String userFullName;

    private String coverImage;

    private String companySize;

    private String industry;

    private String specialization;
}