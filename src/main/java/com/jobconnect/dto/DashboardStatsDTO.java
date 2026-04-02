package com.jobconnect.dto;
import lombok.Data;
import java.util.List;

@Data
public class DashboardStatsDTO {
    private long totalUsers;
    private long totalCompanies;
    private long totalJobs;
    private long totalApplications;
    private List<RecentCompanyDTO> recentCompanies;
}