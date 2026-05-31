package com.jobconnect.repository;

import com.jobconnect.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // Tìm kiếm theo tên (Title) chứa từ khóa, không phân biệt hoa thường
    List<Job> findByTitleContainingIgnoreCase(String title);

    // Lọc theo địa điểm
    List<Job> findByLocation(String location);

    // 1. Lọc job theo trạng thái (VD: Lấy danh sách job "APPROVED" cho trang chủ)
    List<Job> findByStatus(String status);

    // 2. Lấy toàn bộ job của một công ty cụ thể (Để công ty tự quản lý tin của
    // mình)
    List<Job> findByCompanyId(Long companyId);

    // Để đếm tổng số job của 1 công ty
    long countByCompanyId(Long companyId);

    // Hàm tìm kiếm và lọc kết hợp
    @Query("SELECT j FROM Job j WHERE j.status = 'APPROVED' " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            +
            "AND (:location IS NULL OR :location = '' OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) "
            +
            "AND (:category IS NULL OR :category = '' OR LOWER(j.category) = LOWER(:category)) " +
            "AND (:minSalary IS NULL OR j.salary >= :minSalary)")
    Page<Job> searchAndFilterJobs(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("category") String category,
            @Param("minSalary") Long minSalary,
            Pageable pageable);

    // Lọc Job theo trạng thái (Có phân trang cho trang quản lý của Admin)
    Page<Job> findByStatus(String status, Pageable pageable);

    // Tìm Job theo tiêu đề (Có phân trang cho trang quản lý của Admin)
    Page<Job> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Lấy tất cả các Job thuộc về Company mà Company đó lại do User (email) sở hữu
    List<Job> findByCompany_User_Email(String email);

    // --- THÊM VÀO ĐỂ ĐẾM SỐ LƯỢNG JOB ĐANG ACTIVE ---
    @Query("SELECT COUNT(j) FROM Job j WHERE j.company.id = :companyId AND j.status = 'APPROVED'")
    long countActiveJobsByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT DISTINCT j.category FROM Job j " +
            "WHERE j.category IS NOT NULL AND j.category <> '' " +
            "ORDER BY j.category ASC")
    List<String> findDistinctCategories();

    long countByCompanyIdAndStatus(Long companyId, String status);

    @Query("""
                SELECT j FROM Job j
                WHERE j.status = 'APPROVED'
                AND j.id <> :jobId
                AND (
                    LOWER(j.category) = LOWER(:category)
                    OR j.company.id = :companyId
                )
                ORDER BY j.createdAt DESC
            """)
    List<Job> findRelatedJobs(
            @Param("jobId") Long jobId,
            @Param("category") String category,
            @Param("companyId") Long companyId,
            Pageable pageable);

    Page<Job> findByCompany_IdAndStatusOrderByCreatedAtDesc(
            Long companyId,
            String status,
            Pageable pageable);
}