package com.jobconnect.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyRequest {

    private String name;

    private String taxCode;

    private String address;

    private String description;

    private String logo;

    private String coverImage;

    private String website;

    private String phone;

    private String companySize;

    private String industry;

    private String specialization;
}