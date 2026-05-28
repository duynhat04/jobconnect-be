package com.jobconnect.repository;

import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByUserAndJob(User user, Job job);

    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByUserId(Long userId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.job.company.id = :companyId")
    long countTotalCVsByCompanyId(@Param("companyId") Long companyId);

    List<JobApplication> findByJob_Company_User_EmailOrderByIdDesc(String email);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.job.company.id = :companyId AND ja.status = 'PENDING'")
    long countPendingCVsByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.job.company.id = :companyId AND ja.status = 'ACCEPTED'")
    long countApprovedCVsByCompanyId(@Param("companyId") Long companyId);

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
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.skills) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.desiredPosition) LIKE LOWER(CONCAT('%', :keyword, '%'))
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