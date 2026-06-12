package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicMonthlyStatsDTO {

    private long jobsThisMonth;

    private long applicationsThisMonth;

    private long companiesThisMonth;

    private long activeJobs;
}