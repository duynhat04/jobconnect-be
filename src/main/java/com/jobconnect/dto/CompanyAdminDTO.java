package com.jobconnect.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAdminDTO {

    private Long id;

    private String name;

    private String taxCode;

    private String address;

    private String website;

    private String phone;

    private String logo;

    private String status;

    private String email;

    private String createdAt;

    private Integer remainingPosts;

    private String coverImage;

    private String companySize;

    private String industry;

    private String specialization;
}