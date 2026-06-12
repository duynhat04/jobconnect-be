package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicStatsDTO {

    private long activeJobs;

    private long totalCandidates;

    private long totalCompanies;

    private long totalLocations;
}