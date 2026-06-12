package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyListResponse {

    private Long id;
    private String name;
    private String logo;
    private String coverImage;
    private String address;
    private String website;
    private String description;
    private String status;
    private Integer remainingPosts;

    private String companySize;
    private String industry;
    private String specialization;
}