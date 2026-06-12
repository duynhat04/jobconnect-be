package com.jobconnect.service;

import com.jobconnect.dto.CompanyListResponse;
import com.jobconnect.dto.CompanyRequest;
import com.jobconnect.dto.CompanyResponse;
import com.jobconnect.dto.CompanyStatsDTO;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.User;
import com.jobconnect.repository.CompanyRepository;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private CloudinaryStorageService cloudinaryStorageService;

    // EMPLOYER: Đăng ký công ty
    @Transactional
    public CompanyResponse registerCompany(Long userId, CompanyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID " + userId));

        if (companyRepository.existsByUserId(userId)) {
            throw new RuntimeException("Người dùng này đã có công ty hoặc đang chờ duyệt!");
        }

        if (request.getTaxCode() == null || request.getTaxCode().trim().isEmpty()) {
            throw new RuntimeException("Mã số thuế không được để trống!");
        }

        if (companyRepository.existsByTaxCode(request.getTaxCode().trim())) {
            throw new RuntimeException("Mã số thuế này đã tồn tại trên hệ thống!");
        }

        Company company = new Company();
        company.setName(normalize(request.getName()));
        company.setTaxCode(normalize(request.getTaxCode()));
        company.setAddress(normalize(request.getAddress()));
        company.setDescription(normalize(request.getDescription()));
        company.setLogo(normalize(request.getLogo()));
        company.setWebsite(normalize(request.getWebsite()));
        company.setPhone(normalize(request.getPhone()));
        company.setCoverImage(normalize(request.getCoverImage()));
        company.setCompanySize(normalize(request.getCompanySize()));
        company.setIndustry(normalize(request.getIndustry()));
        company.setSpecialization(normalize(request.getSpecialization()));
        company.setUser(user);
        company.setStatus("PENDING");
        company.setRemainingPosts(5);

        Company savedCompany = companyRepository.save(company);

        return toCompanyResponse(savedCompany);
    }

    // ADMIN: Duyệt công ty
    @Transactional
    public CompanyResponse approveCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty với ID " + companyId));

        if (!"PENDING".equalsIgnoreCase(company.getStatus())) {
            throw new RuntimeException("Công ty này đã được duyệt hoặc đã bị từ chối!");
        }

        company.setStatus("APPROVED");

        User user = company.getUser();
        if (user != null) {
            user.setRole("EMPLOYER");
            userRepository.save(user);
        }

        Company savedCompany = companyRepository.save(company);

        return toCompanyResponse(savedCompany);
    }

    // EMPLOYER: Lấy công ty của tài khoản đang đăng nhập
    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany(String email) {
        Company company = companyRepository.findByUser_Email(email)
                .orElseThrow(
                        () -> new RuntimeException("Bạn chưa đăng ký thông tin công ty hoặc tài khoản không tồn tại!"));

        return toCompanyResponse(company);
    }

    // EMPLOYER: Thống kê dashboard
    @Transactional(readOnly = true)
    public CompanyStatsDTO getCompanyStats(String email) {
        Company company = companyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty của bạn!"));

        Long companyId = company.getId();

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
                .profileViews(125L)
                .responseRate("88%")
                .build();
    }

    // EMPLOYER: Cập nhật công ty
    @Transactional
    public CompanyResponse updateMyCompany(String email, CompanyRequest request) {
        Company existingCompany = companyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty của bạn!"));

        if (request.getName() != null) {
            existingCompany.setName(normalize(request.getName()));
        }

        if (request.getAddress() != null) {
            existingCompany.setAddress(normalize(request.getAddress()));
        }

        if (request.getDescription() != null) {
            existingCompany.setDescription(normalize(request.getDescription()));
        }

        if (request.getLogo() != null) {
            existingCompany.setLogo(normalize(request.getLogo()));
        }

        if (request.getWebsite() != null) {
            existingCompany.setWebsite(normalize(request.getWebsite()));
        }

        if (request.getPhone() != null) {
            existingCompany.setPhone(normalize(request.getPhone()));
        }
        if (request.getCoverImage() != null) {
            existingCompany.setCoverImage(normalize(request.getCoverImage()));
        }

        if (request.getCompanySize() != null) {
            existingCompany.setCompanySize(normalize(request.getCompanySize()));
        }

        if (request.getIndustry() != null) {
            existingCompany.setIndustry(normalize(request.getIndustry()));
        }

        if (request.getSpecialization() != null) {
            existingCompany.setSpecialization(normalize(request.getSpecialization()));
        }

        Company savedCompany = companyRepository.save(existingCompany);

        return toCompanyResponse(savedCompany);
    }

    // PUBLIC: Danh sách công ty đã duyệt
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

    // PUBLIC: Chi tiết công ty đã duyệt
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
                company.getCoverImage(),
                company.getAddress(),
                company.getWebsite(),
                company.getDescription(),
                company.getStatus(),
                company.getRemainingPosts(),
                company.getCompanySize(),
                company.getIndustry(),
                company.getSpecialization());
    }

    private CompanyResponse toCompanyResponse(Company company) {
        User user = company.getUser();

        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .taxCode(company.getTaxCode())
                .address(company.getAddress())
                .description(company.getDescription())
                .logo(company.getLogo())
                .website(company.getWebsite())
                .phone(company.getPhone())
                .coverImage(company.getCoverImage())
                .companySize(company.getCompanySize())
                .industry(company.getIndustry())
                .specialization(company.getSpecialization())
                .status(company.getStatus())
                .remainingPosts(company.getRemainingPosts())
                .createdAt(company.getCreatedAt())
                .userId(user != null ? user.getId() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public CompanyResponse uploadMyCompanyLogo(String email, MultipartFile file) {
        Company company = companyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty của bạn!"));

        String logoUrl = cloudinaryStorageService.uploadCompanyLogo(file, company.getId());

        company.setLogo(logoUrl);

        Company savedCompany = companyRepository.save(company);

        return toCompanyResponse(savedCompany);
    }

    @Transactional
    public CompanyResponse uploadMyCompanyCoverImage(String email, MultipartFile file) {
        Company company = companyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty của bạn!"));

        String coverImageUrl = cloudinaryStorageService.uploadCompanyCover(file, company.getId());

        company.setCoverImage(coverImageUrl);

        Company savedCompany = companyRepository.save(company);

        return toCompanyResponse(savedCompany);
    }
}