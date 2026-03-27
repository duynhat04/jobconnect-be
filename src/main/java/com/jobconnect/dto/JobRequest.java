package com.jobconnect.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobRequest {
    private String title;
    private String description;
    private String location;
    private Long salary;
    private Long companyId;
}