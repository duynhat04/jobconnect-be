package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    private long totalUsers;

    private long totalCompanies;

    private long totalJobs;

    private long totalApplications;

    private long totalReports;

    private List<PendingApprovalDTO> pendingApprovals;
}