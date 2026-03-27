package com.jobconnect.service;

import com.jobconnect.entity.Company;
import com.jobconnect.entity.User;
import com.jobconnect.repository.CompanyRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    public Company registerCompany(Long userId, Company company) {
        // Chốt chặn 1: Kiểm tra User có tồn tại trong hệ thống không
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy người dùng với ID " + userId));

        // Chốt chặn 2: Chống Spam. Mỗi User chỉ được tạo 1 Công ty (Dù PENDING hay APPROVED)
        if (companyRepository.existsByUserId(userId)) {
            throw new RuntimeException("Lỗi: Người dùng này đã có công ty hoặc đang chờ duyệt!");
        }

        // Chốt chặn 3: Kiểm tra Mã số thuế (Tránh công ty ảo fake công ty thật)
        if (companyRepository.existsByTaxCode(company.getTaxCode())) {
            throw new RuntimeException("Lỗi: Mã số thuế này đã tồn tại trên hệ thống!");
        }

        // Vượt qua 3 chốt chặn -> Gắn User vào Company và thiết lập PENDING
        company.setUser(user);
        company.setStatus("PENDING");

        return companyRepository.save(company);
    }
    // API dành cho Admin: Duyệt công ty
    public Company approveCompany(Long companyId) {
        // 1. Tìm công ty theo ID
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy công ty với ID " + companyId));

        // 2. Kiểm tra xem công ty có đang ở trạng thái chờ duyệt không
        if (!company.getStatus().equals("PENDING")) {
            throw new RuntimeException("Lỗi: Công ty này đã được duyệt hoặc đã bị từ chối!");
        }

        // 3. Đổi trạng thái Công ty thành APPROVED
        company.setStatus("APPROVED");

        // 4. Lấy User là chủ của công ty này và nâng cấp lên làm Nhà tuyển dụng (EMPLOYER)
        User user = company.getUser();
        if (user != null) {
            user.setRole("EMPLOYER");
            userRepository.save(user); // Cập nhật User vào Database
        }

        // 5. Lưu lại thông tin Công ty
        return companyRepository.save(company);
    }
}