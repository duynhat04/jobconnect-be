package com.jobconnect.repository;

import com.jobconnect.dto.JobApplicationCountProjection;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByUserAndJob(User user, Job job);

    // =========================
    // COUNT BY JOB
    // =========================

    long countByJobId(Long jobId);

    long countByJobIdAndStatus(Long jobId, String status);

    List<JobApplication> findByJobIdAndStatusOrderByAppliedAtDesc(
            Long jobId,
            String status
    );

    // =========================
    // COUNT MULTIPLE JOBS
    // Tối ưu tránh N+1 query khi list nhiều job
    // =========================

    @Query("""
            SELECT 
                ja.job.id AS jobId,
                COUNT(ja.id) AS applicationCount,
                COALESCE(SUM(CASE WHEN ja.status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pendingApplicationCount,
                COALESCE(SUM(CASE WHEN ja.status = 'REVIEWED' THEN 1 ELSE 0 END), 0) AS reviewedApplicationCount,
                COALESCE(SUM(CASE WHEN ja.status = 'ACCEPTED' THEN 1 ELSE 0 END), 0) AS acceptedApplicationCount,
                COALESCE(SUM(CASE WHEN ja.status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS rejectedApplicationCount
            FROM JobApplication ja
            WHERE ja.job.id IN :jobIds
            GROUP BY ja.job.id
            """)
    List<JobApplicationCountProjection> countApplicationsByJobIds(
            @Param("jobIds") List<Long> jobIds
    );

    // =========================
    // PUBLIC MONTHLY STATS
    // Dùng cho /api/public/monthly-stats
    // =========================

    @Query("""
            SELECT COUNT(ja) FROM JobApplication ja
            WHERE ja.appliedAt >= :startOfMonth
            AND ja.appliedAt < :startOfNextMonth
            """)
    long countApplicationsThisMonth(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("startOfNextMonth") LocalDateTime startOfNextMonth
    );

    // =========================
    // FIND WITH RELATIONS
    // =========================

    @EntityGraph(attributePaths = {
            "user",
            "job",
            "job.company",
            "job.company.user"
    })
    Optional<JobApplication> findWithRelationsById(Long id);

    @EntityGraph(attributePaths = {
            "user",
            "job",
            "job.company"
    })
    List<JobApplication> findByUserIdOrderByAppliedAtDesc(Long userId);

    @EntityGraph(attributePaths = {
            "user",
            "job",
            "job.company",
            "job.company.user"
    })
    List<JobApplication> findByJobIdOrderByAppliedAtDesc(Long jobId);

    @EntityGraph(attributePaths = {
            "user",
            "job",
            "job.company",
            "job.company.user"
    })
    List<JobApplication> findByJob_Company_User_EmailOrderByAppliedAtDesc(
            String email
    );

    // =========================
    // DASHBOARD COUNT BY COMPANY
    // =========================

    @Query("""
            SELECT COUNT(ja) FROM JobApplication ja
            WHERE ja.job.company.id = :companyId
            """)
    long countTotalCVsByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT COUNT(ja) FROM JobApplication ja
            WHERE ja.job.company.id = :companyId
            AND ja.status = 'PENDING'
            """)
    long countPendingCVsByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT COUNT(ja) FROM JobApplication ja
            WHERE ja.job.company.id = :companyId
            AND ja.status = 'ACCEPTED'
            """)
    long countApprovedCVsByCompanyId(@Param("companyId") Long companyId);

    // =========================
    // SEARCH CANDIDATES FOR JOB
    // =========================

    @EntityGraph(attributePaths = {
            "user",
            "job",
            "job.company",
            "job.company.user"
    })
    @Query("""
            SELECT ja FROM JobApplication ja
            JOIN ja.job j
            JOIN j.company c
            JOIN c.user employer
            JOIN ja.user u
            WHERE j.id = :jobId
            AND employer.email = :employerEmail

            AND (
                :status IS NULL OR :status = ''
                OR LOWER(ja.status) = LOWER(:status)
            )

            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(ja.candidateName, u.fullName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(ja.candidateEmail, u.email)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(ja.candidateSkills, u.skills)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(ja.candidateDesiredPosition, u.desiredPosition)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )

            ORDER BY ja.appliedAt DESC
            """)
    List<JobApplication> findCandidatesForJobWithFilter(
            @Param("jobId") Long jobId,
            @Param("employerEmail") String employerEmail,
            @Param("keyword") String keyword,
            @Param("status") String status
    );
}