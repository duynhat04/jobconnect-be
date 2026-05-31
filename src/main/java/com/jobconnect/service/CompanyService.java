package com.jobconnect.service;

import com.jobconnect.dto.CompanyStatsDTO;
import com.jobconnect.dto.CompanyListResponse;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.User;
import com.jobconnect.repository.CompanyRepository;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    public Company registerCompany(Long userId, Company company) {
        // Chốt chặn 1: Kiểm tra User có tồn tại trong hệ thống không
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy người dùng với ID " + userId));

        // Chốt chặn 2: Chống Spam. Mỗi User chỉ được tạo 1 Công ty (Dù PENDING hay
        // APPROVED)
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

        // FREE PLAN mặc định
        if (company.getRemainingPosts() == null) {
            company.setRemainingPosts(5);
        }

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

        // 4. Lấy User là chủ của công ty này và nâng cấp lên làm Nhà tuyển dụng
        // (EMPLOYER)
        User user = company.getUser();
        if (user != null) {
            user.setRole("EMPLOYER");
            userRepository.save(user); // Cập nhật User vào Database
        }

        // 5. Lưu lại thông tin Công ty
        return companyRepository.save(company);
    }

    public Company getMyCompany(String email) {
        return companyRepository.findByUser_Email(email)
                .orElseThrow(
                        () -> new RuntimeException("Bạn chưa đăng ký thông tin công ty hoặc tài khoản không tồn tại!"));
    }

    public CompanyStatsDTO getCompanyStats(String email) {
        Company company = companyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty của bạn!"));

        Long companyId = company.getId();

        // Count thật từ Database
        long activeJobs = jobRepository.countActiveJobsByCompanyId(companyId);
        long totalCVs = jobApplicationRepository.countTotalCVsByCompanyId(companyId);
        long pending = jobApplicationRepository.countPendingCVsByCompanyId(companyId);
        long approved = jobApplicationRepository.countApprovedCVsByCompanyId(companyId);
        long pendingJobs = jobRepository.countByCompanyIdAndStatus(companyId, "PENDING");

        return CompanyStatsDTO.builder()
                .activeJobs(activeJobs)
                .totalCVs(totalCVs)
                .pendingCVs(pending)
                .approvedCVs(approved)
                .pendingJobs(pendingJobs)
                .remainingPosts(company.getRemainingPosts() != null ? company.getRemainingPosts() : 0)
                .profileViews(125L) // Sau này sếp làm bảng Tracking thì thay vào đây
                .responseRate("88%")
                .build();
    }

    // API Cập nhật thông tin công ty
    public Company updateMyCompany(String email, Company updatedData) {
        Company existingCompany = companyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty của bạn!"));

        // Cập nhật các trường cho phép (Không cho phép đổi Mã số thuế, Trạng thái
        // duyệt)
        if (updatedData.getName() != null)
            existingCompany.setName(updatedData.getName());
        if (updatedData.getAddress() != null)
            existingCompany.setAddress(updatedData.getAddress());
        if (updatedData.getDescription() != null)
            existingCompany.setDescription(updatedData.getDescription());
        if (updatedData.getLogo() != null)
            existingCompany.setLogo(updatedData.getLogo());
        if (updatedData.getWebsite() != null)
            existingCompany.setWebsite(updatedData.getWebsite());

        return companyRepository.save(existingCompany);
    }

    @Transactional(readOnly = true)
    public Page<CompanyListResponse> getPublicCompanies(String keyword, int page, int size) {
        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 9;
        }

        if (size > 30) {
            size = 30;
        }

        Pageable pageable = PageRequest.of(page, size);

        String cleanKeyword = keyword == null ? null : keyword.trim();

        Page<Company> companies;

        if (cleanKeyword == null || cleanKeyword.isEmpty()) {
            companies = companyRepository.findByStatusOrderByIdDesc("APPROVED", pageable);
        } else {
            companies = companyRepository.findByNameContainingIgnoreCaseAndStatusOrderByIdDesc(
                    cleanKeyword,
                    "APPROVED",
                    pageable);
        }

        return companies.map(this::toCompanyListResponse);
    }

    @Transactional(readOnly = true)
    public CompanyListResponse getPublicCompanyDetail(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty!"));

        if (!"APPROVED".equalsIgnoreCase(company.getStatus())) {
            throw new RuntimeException("Công ty chưa được duyệt hoặc không khả dụng!");
        }

        return toCompanyListResponse(company);
    }

    private CompanyListResponse toCompanyListResponse(Company company) {
        return new CompanyListResponse(
                company.getId(),
                company.getName(),
                company.getLogo(),
                company.getAddress(),
                company.getWebsite(),
                company.getDescription(),
                company.getStatus(),
                company.getRemainingPosts());
    }
}