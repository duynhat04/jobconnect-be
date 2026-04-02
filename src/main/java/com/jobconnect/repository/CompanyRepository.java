package com.jobconnect.repository;

import com.jobconnect.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    // Kiểm tra xem Mã số thuế đã ai đăng ký chưa
    boolean existsByTaxCode(String taxCode);

    // Kiểm tra xem User này đã gửi yêu cầu tạo công ty bao giờ chưa
    boolean existsByUserId(Long userId);

    List<Company> findTop5ByOrderByIdDesc();

    // Lấy danh sách công ty theo trạng thái (có phân trang)
    Page<Company> findByStatus(String status, Pageable pageable);

    // Tìm kiếm công ty theo tên (có phân trang)
    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);

}