package com.jobconnect.repository;

import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByUserAndJob(User user, Job job);

    // Để sau này Nhà tuyển dụng xem danh sách CV nộp vào Job của họ
    List<JobApplication> findByJobId(Long jobId);

    // Để Ứng viên xem lịch sử những Job mình đã nộp
    List<JobApplication> findByUserId(Long userId);

    // --- THÊM VÀO ĐỂ ĐẾM TỔNG SỐ CV CỦA CÔNG TY ---
    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.job.company.id = :companyId")
    long countTotalCVsByCompanyId(@org.springframework.data.repository.query.Param("companyId") Long companyId);

    // Lấy danh sách tất cả ứng viên nộp vào công ty của nhà tuyển dụng (Sắp xếp mới
    // nhất lên đầu)
    List<JobApplication> findByJob_Company_User_EmailOrderByIdDesc(String email);

    // Đếm số CV đang chờ duyệt (PENDING)
    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.job.company.id = :companyId AND ja.status = 'PENDING'")
    long countPendingCVsByCompanyId(@Param("companyId") Long companyId);

    // Đếm số CV đã duyệt/chấp nhận (ACCEPTED)
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
            JOIN ja.user u
            JOIN ja.job j
            JOIN j.company c
            JOIN c.user employer
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

            AND (
                :skill IS NULL OR :skill = ''
                OR LOWER(u.skills) LIKE LOWER(CONCAT('%', :skill, '%'))
            )

            AND (
                :minExperience IS NULL
                OR (u.experienceYears IS NOT NULL AND u.experienceYears >= :minExperience)
            )

            AND (
                :maxExpectedSalary IS NULL
                OR u.expectedSalary IS NULL
                OR u.expectedSalary <= :maxExpectedSalary
            )

            AND (
                :workType IS NULL OR :workType = ''
                OR LOWER(u.workType) = LOWER(:workType)
            )

            AND (
                :educationLevel IS NULL OR :educationLevel = ''
                OR LOWER(u.educationLevel) LIKE LOWER(CONCAT('%', :educationLevel, '%'))
            )

            AND (
                :englishLevel IS NULL OR :englishLevel = ''
                OR LOWER(u.englishLevel) LIKE LOWER(CONCAT('%', :englishLevel, '%'))
            )

            ORDER BY ja.appliedAt DESC
            """)
    List<JobApplication> searchCandidatesForJob(
            @Param("jobId") Long jobId,
            @Param("employerEmail") String employerEmail,
            @Param("keyword") String keyword,
            @Param("skill") String skill,
            @Param("minExperience") Integer minExperience,
            @Param("maxExpectedSalary") Long maxExpectedSalary,
            @Param("workType") String workType,
            @Param("educationLevel") String educationLevel,
            @Param("englishLevel") String englishLevel,
            @Param("status") String status);

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
            @Param("status") String status);
}