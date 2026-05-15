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
public class RevenueStatsDTO {
    private SummaryDTO summary;
    private List<ChartDataDTO> chartData;
    private List<RecentTransactionDTO> recentTransactions;
}