package com.jobconnect.controller;

import com.jobconnect.dto.PublicMonthlyStatsDTO;
import com.jobconnect.dto.PublicStatsDTO;
import com.jobconnect.repository.CompanyRepository;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PublicStatsController {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobApplicationRepository jobApplicationRepository;

    @GetMapping("/stats")
    public PublicStatsDTO getPublicStats() {
        return PublicStatsDTO.builder()
                .activeJobs(jobRepository.countActivePublicJobs())
                .totalCandidates(userRepository.countByRole("CANDIDATE"))
                .totalCompanies(companyRepository.countByStatus("APPROVED"))
                .totalLocations(jobRepository.countDistinctActiveLocations())
                .build();
    }

    @GetMapping("/monthly-stats")
    public PublicMonthlyStatsDTO getMonthlyStats() {
        LocalDate today = LocalDate.now();

        LocalDateTime startOfMonth = today
                .withDayOfMonth(1)
                .atStartOfDay();

        LocalDateTime startOfNextMonth = today
                .withDayOfMonth(1)
                .plusMonths(1)
                .atStartOfDay();

        return PublicMonthlyStatsDTO.builder()
                .jobsThisMonth(
                        jobRepository.countApprovedJobsThisMonth(
                                startOfMonth,
                                startOfNextMonth
                        )
                )
                .applicationsThisMonth(
                        jobApplicationRepository.countApplicationsThisMonth(
                                startOfMonth,
                                startOfNextMonth
                        )
                )
                .companiesThisMonth(
                        companyRepository.countApprovedCompaniesThisMonth(
                                startOfMonth,
                                startOfNextMonth
                        )
                )
                .activeJobs(jobRepository.countActivePublicJobs())
                .build();
    }
}