package com.jobconnect.dto;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class CompanyStatsDTO {
    private long activeJobs;   
    private long totalCVs;     
    private long pendingCVs;  
    private long approvedCVs;  
    private long pendingJobs;
    private int remainingPosts;
    private long profileViews; 
    private String responseRate;
}
