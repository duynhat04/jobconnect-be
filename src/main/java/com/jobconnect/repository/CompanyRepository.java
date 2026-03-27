package com.jobconnect.repository;

import com.jobconnect.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    // Kiểm tra xem Mã số thuế đã ai đăng ký chưa
    boolean existsByTaxCode(String taxCode);

    // Kiểm tra xem User này đã gửi yêu cầu tạo công ty bao giờ chưa
    boolean existsByUserId(Long userId);
}