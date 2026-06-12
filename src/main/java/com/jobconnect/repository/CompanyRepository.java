package com.jobconnect.repository;

import com.jobconnect.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    // =========================
    // BASIC QUERY
    // =========================

    boolean existsByTaxCode(String taxCode);

    boolean existsByUserId(Long userId);

    long countByStatus(String status);

    List<Company> findTop5ByOrderByIdDesc();

    Page<Company> findByStatus(String status, Pageable pageable);

    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Company> findByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<Company> findByNameContainingIgnoreCaseAndStatusOrderByIdDesc(
            String name,
            String status,
            Pageable pageable
    );

    Optional<Company> findByUser_Email(String email);

    // =========================
    // ADMIN QUERY WITH USER
    // Dùng riêng cho admin cần lấy company.user.email
    // Tránh lỗi lazy loading: could not initialize proxy User - no Session
    // Không sửa các method cũ để tránh ảnh hưởng public API
    // =========================

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT c FROM Company c
            """)
    Page<Company> findAllWithUser(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT c FROM Company c
            WHERE c.status = :status
            """)
    Page<Company> findByStatusWithUser(
            @Param("status") String status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT c FROM Company c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    Page<Company> findByNameContainingIgnoreCaseWithUser(
            @Param("name") String name,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT c FROM Company c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
            AND c.status = :status
            """)
    Page<Company> findByNameContainingIgnoreCaseAndStatusWithUser(
            @Param("name") String name,
            @Param("status") String status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT c FROM Company c
            WHERE c.id = :id
            """)
    Optional<Company> findByIdWithUser(@Param("id") Long id);

    // =========================
    // PUBLIC MONTHLY STATS
    // Dùng cho /api/public/monthly-stats
    // =========================

    @Query("""
            SELECT COUNT(c) FROM Company c
            WHERE c.status = 'APPROVED'
            AND c.createdAt >= :startOfMonth
            AND c.createdAt < :startOfNextMonth
            """)
    long countApprovedCompaniesThisMonth(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("startOfNextMonth") LocalDateTime startOfNextMonth
    );
}