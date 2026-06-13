package com.jobconnect.repository;

import com.jobconnect.entity.EmploymentType;
import com.jobconnect.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // =========================
    // BASIC QUERY
    // =========================

    List<Job> findByTitleContainingIgnoreCase(String title);

    List<Job> findByLocation(String location);

    List<Job> findByStatus(String status);

    Page<Job> findByStatus(String status, Pageable pageable);

    Page<Job> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    List<Job> findByCompanyId(Long companyId);

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndStatus(Long companyId, String status);

    List<Job> findByCompany_User_Email(String email);

    // =========================
    // FETCH COMPANY
    // Tránh lỗi lazy loading khi map DTO cần company
    // =========================

    @Query("""
            SELECT j FROM Job j
            JOIN FETCH j.company c
            WHERE j.id = :id
            """)
    Optional<Job> findByIdWithCompany(@Param("id") Long id);

    @Query("""
            SELECT j FROM Job j
            JOIN FETCH j.company c
            JOIN c.user u
            WHERE u.email = :email
            ORDER BY j.createdAt DESC
            """)
    List<Job> findMyJobsWithCompany(@Param("email") String email);

    @Query("""
            SELECT j FROM Job j
            JOIN FETCH j.company c
            WHERE j.status = :status
            AND j.expiredAt >= :today
            ORDER BY j.createdAt DESC
            """)
    List<Job> findApprovedJobsWithCompany(
            @Param("status") String status,
            @Param("today") LocalDate today);

    @Query("""
            SELECT j FROM Job j
            JOIN FETCH j.company c
            WHERE j.id = :jobId
            """)
    Optional<Job> findRelatedSourceJobWithCompany(@Param("jobId") Long jobId);

    // =========================
    // PUBLIC SEARCH / FILTER
    // EntityGraph fetch company để map JobResponse, không dùng JOIN FETCH với Page
    // =========================

    @EntityGraph(attributePaths = { "company" })
    @Query("""
            SELECT j FROM Job j
            WHERE j.status = 'APPROVED'
            AND j.expiredAt >= CURRENT_DATE

            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(j.requirements) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )

            AND (
                :location IS NULL OR :location = ''
                OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))
            )

            AND (
                :category IS NULL OR :category = ''
                OR LOWER(j.category) = LOWER(:category)
            )

            AND (
                :minSalary IS NULL OR j.salary >= :minSalary
            )

            AND (
                :employmentType IS NULL OR j.employmentType = :employmentType
            )
            """)
    Page<Job> searchAndFilterJobs(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("category") String category,
            @Param("minSalary") Long minSalary,
            @Param("employmentType") EmploymentType employmentType,
            Pageable pageable);

    // =========================
    // COUNT / CATEGORY
    // =========================

    @Query("""
            SELECT COUNT(j) FROM Job j
            WHERE j.company.id = :companyId
            AND j.status = 'APPROVED'
            AND j.expiredAt >= CURRENT_DATE
            """)
    long countActiveJobsByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT DISTINCT j.category FROM Job j
            WHERE j.category IS NOT NULL
            AND TRIM(j.category) <> ''
            AND j.status = 'APPROVED'
            AND j.expiredAt >= CURRENT_DATE
            ORDER BY j.category ASC
            """)
    List<String> findDistinctCategories();

    @Query("""
            SELECT COUNT(j) FROM Job j
            WHERE j.status = 'APPROVED'
            AND j.expiredAt >= CURRENT_DATE
            """)
    long countActivePublicJobs();

    @Query("""
            SELECT COUNT(DISTINCT j.location) FROM Job j
            WHERE j.status = 'APPROVED'
            AND j.expiredAt >= CURRENT_DATE
            AND j.location IS NOT NULL
            AND TRIM(j.location) <> ''
            """)
    long countDistinctActiveLocations();

    // =========================
    // PUBLIC MONTHLY STATS
    // Dùng cho /api/public/monthly-stats
    // =========================

    @Query("""
            SELECT COUNT(j) FROM Job j
            WHERE j.status = 'APPROVED'
            AND j.createdAt >= :startOfMonth
            AND j.createdAt < :startOfNextMonth
            """)
    long countApprovedJobsThisMonth(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("startOfNextMonth") LocalDateTime startOfNextMonth);

    // =========================
    // RELATED JOBS
    // =========================

    @Query("""
            SELECT j FROM Job j
            JOIN FETCH j.company c
            WHERE j.status = 'APPROVED'
            AND j.expiredAt >= CURRENT_DATE
            AND j.id <> :jobId
            AND (
                (:category IS NOT NULL AND LOWER(j.category) = LOWER(:category))
                OR c.id = :companyId
            )
            ORDER BY j.createdAt DESC
            """)
    List<Job> findRelatedJobs(
            @Param("jobId") Long jobId,
            @Param("category") String category,
            @Param("companyId") Long companyId,
            Pageable pageable);

    // =========================
    // PUBLIC JOBS BY COMPANY
    // =========================

    @EntityGraph(attributePaths = { "company" })
    Page<Job> findByCompany_IdAndStatusAndExpiredAtGreaterThanEqualOrderByCreatedAtDesc(
            Long companyId,
            String status,
            LocalDate today,
            Pageable pageable);

    // =========================
    // EMPLOYER JOB MANAGEMENT
    // =========================

    @EntityGraph(attributePaths = { "company" })
    Page<Job> findByCompany_IdOrderByCreatedAtDesc(
            Long companyId,
            Pageable pageable);

    // =========================
    // ADMIN JOB MANAGEMENT WITH COMPANY
    // Dùng riêng cho admin để không trả Job entity bị lazy company
    // =========================

    @EntityGraph(attributePaths = { "company" })
    @Query("""
            SELECT j FROM Job j
            """)
    Page<Job> findAllWithCompany(Pageable pageable);

    @EntityGraph(attributePaths = { "company" })
    @Query("""
            SELECT j FROM Job j
            WHERE j.status = :status
            """)
    Page<Job> findByStatusWithCompany(
            @Param("status") String status,
            Pageable pageable);

    @EntityGraph(attributePaths = { "company" })
    @Query("""
            SELECT j FROM Job j
            WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))
            """)
    Page<Job> findByTitleContainingIgnoreCaseWithCompany(
            @Param("title") String title,
            Pageable pageable);

    @EntityGraph(attributePaths = { "company" })
    @Query("""
            SELECT j FROM Job j
            WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))
            AND j.status = :status
            """)
    Page<Job> findByTitleContainingIgnoreCaseAndStatusWithCompany(
            @Param("title") String title,
            @Param("status") String status,
            Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Job j
            SET j.status = 'EXPIRED'
            WHERE j.status = 'APPROVED'
            AND j.expiredAt < CURRENT_DATE
            """)
    int markExpiredApprovedJobs();

    @Query("""
            SELECT j FROM Job j
            WHERE j.status = 'EXPIRED'
            ORDER BY j.expiredAt DESC
            """)
    List<Job> findExpiredJobs();

    long countByStatus(String status);

    @EntityGraph(attributePaths = { "company" })
    @Query("""
            SELECT j FROM Job j
            WHERE j.status = 'APPROVED'
            AND j.expiredAt >= CURRENT_DATE
            ORDER BY j.createdAt DESC
            """)
    Page<Job> findAiSearchablePublicJobs(Pageable pageable);

    @Query(value = """
            WITH query_data AS (
                SELECT websearch_to_tsquery(
                    'simple',
                    public.immutable_unaccent(CAST(:keyword AS text))
                ) AS query
            )
            SELECT j.*
            FROM jobs j
            CROSS JOIN query_data q
            WHERE j.status = 'APPROVED'
            AND j.expired_at >= CURRENT_DATE
            AND (
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.title, ''))), 'A') ||
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.requirements, ''))), 'B') ||
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.description, ''))), 'C') ||
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.category, ''))), 'C') ||
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.location, ''))), 'D')
            ) @@ q.query
            ORDER BY
                ts_rank_cd(
                    (
                        setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.title, ''))), 'A') ||
                        setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.requirements, ''))), 'B') ||
                        setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.description, ''))), 'C') ||
                        setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.category, ''))), 'C') ||
                        setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.location, ''))), 'D')
                    ),
                    q.query
                ) DESC,
                j.created_at DESC
            """, countQuery = """
            WITH query_data AS (
                SELECT websearch_to_tsquery(
                    'simple',
                    public.immutable_unaccent(CAST(:keyword AS text))
                ) AS query
            )
            SELECT COUNT(*)
            FROM jobs j
            CROSS JOIN query_data q
            WHERE j.status = 'APPROVED'
            AND j.expired_at >= CURRENT_DATE
            AND (
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.title, ''))), 'A') ||
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.requirements, ''))), 'B') ||
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.description, ''))), 'C') ||
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.category, ''))), 'C') ||
                setweight(to_tsvector('simple', public.immutable_unaccent(coalesce(j.location, ''))), 'D')
            ) @@ q.query
            """, nativeQuery = true)
    Page<Job> searchAiPublicJobsFullText(
            @Param("keyword") String keyword,
            Pageable pageable);
}