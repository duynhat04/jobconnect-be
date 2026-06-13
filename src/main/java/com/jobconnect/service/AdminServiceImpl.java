package com.jobconnect.service;

import com.jobconnect.dto.DashboardStatsDTO;
import com.jobconnect.dto.RecentCompanyDTO;
import com.jobconnect.dto.RevenueStatsDTO;
import com.jobconnect.dto.SummaryDTO;
import com.jobconnect.dto.ChartDataDTO;
import com.jobconnect.dto.RecentTransactionDTO;
import com.jobconnect.dto.CompanyAdminDTO;
import com.jobconnect.dto.JobAdminDTO;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobPackage;
import com.jobconnect.entity.User;
import com.jobconnect.entity.UserCV;
import com.jobconnect.entity.Transaction;

import com.jobconnect.repository.*;
import com.jobconnect.dto.PendingApprovalDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserCVRepository userCVRepository;
    private final TransactionRepository transactionRepository;
    private final JobPackageRepository jobPackageRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();
        long totalJobs = jobRepository.count();
        long totalApplications = jobApplicationRepository.count();

        List<PendingApprovalDTO> pendingApprovals = new ArrayList<>();

        List<Company> pendingCompanies = companyRepository
                .findByStatusOrderByIdDesc("PENDING", PageRequest.of(0, 5))
                .getContent();

        for (Company company : pendingCompanies) {
            pendingApprovals.add(
                    PendingApprovalDTO.builder()
                            .id(company.getId())
                            .name(company.getName())
                            .type("COMPANY")
                            .status(company.getStatus())
                            .build());
        }

        List<Job> pendingJobs = jobRepository
                .findByStatus("PENDING", PageRequest.of(0, 5))
                .getContent();

        for (Job job : pendingJobs) {
            pendingApprovals.add(
                    PendingApprovalDTO.builder()
                            .id(job.getId())
                            .name(job.getTitle())
                            .type("JOB")
                            .status(job.getStatus())
                            .build());
        }

        List<PendingApprovalDTO> limitedPendingApprovals = pendingApprovals
                .stream()
                .limit(5)
                .toList();

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalCompanies(totalCompanies)
                .totalJobs(totalJobs)
                .totalApplications(totalApplications)
                .totalReports(0)
                .pendingApprovals(limitedPendingApprovals)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyAdminDTO> getAllCompanies(int page, int size, String status, String search) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by("id").descending());

        String keyword = search != null ? search.trim() : "";
        String companyStatus = status != null ? status.trim().toUpperCase() : "";

        Page<Company> companies;

        if (!keyword.isEmpty() && !companyStatus.isEmpty()) {
            companies = companyRepository.findByNameContainingIgnoreCaseAndStatusWithUser(
                    keyword,
                    companyStatus,
                    pageable);
        } else if (!keyword.isEmpty()) {
            companies = companyRepository.findByNameContainingIgnoreCaseWithUser(
                    keyword,
                    pageable);
        } else if (!companyStatus.isEmpty()) {
            companies = companyRepository.findByStatusWithUser(
                    companyStatus,
                    pageable);
        } else {
            companies = companyRepository.findAllWithUser(pageable);
        }

        return companies.map(this::mapToCompanyAdminDTO);
    }

    private CompanyAdminDTO mapToCompanyAdminDTO(Company company) {
        User user = company.getUser();

        return CompanyAdminDTO.builder()
                .id(company.getId())
                .name(company.getName())
                .taxCode(company.getTaxCode())
                .address(company.getAddress())
                .website(company.getWebsite())
                .phone(company.getPhone())
                .logo(company.getLogo())
                .status(company.getStatus())
                .email(user != null ? user.getEmail() : "Không có email")
                .createdAt(
                        company.getCreatedAt() != null
                                ? company.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                : null)
                .remainingPosts(company.getRemainingPosts())
                .build();
    }

    @Override
    @Transactional
    public CompanyAdminDTO updateCompanyStatus(Long companyId, String status) {
        String newStatus = status != null ? status.trim().toUpperCase() : "";

        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(newStatus)) {
            throw new RuntimeException("Trạng thái công ty không hợp lệ!");
        }

        Company company = companyRepository.findByIdWithUser(companyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty với ID: " + companyId));

        company.setStatus(newStatus);

        User user = company.getUser();

        if ("APPROVED".equals(newStatus)) {
            if (user != null && !"EMPLOYER".equalsIgnoreCase(user.getRole())) {
                user.setRole("EMPLOYER");
            }
        }

        if ("REJECTED".equals(newStatus)) {
            if (user != null && "EMPLOYER".equalsIgnoreCase(user.getRole())) {
                user.setRole("CANDIDATE");
            }
        }

        Company savedCompany = companyRepository.save(company);

        return mapToCompanyAdminDTO(savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobAdminDTO> getAllJobs(int page, int size, String status, String search) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by("id").descending());

        String keyword = search != null ? search.trim() : "";
        String jobStatus = status != null ? status.trim().toUpperCase() : "";

        Page<Job> jobs;

        if (!keyword.isEmpty() && !jobStatus.isEmpty()) {
            jobs = jobRepository.findByTitleContainingIgnoreCaseAndStatusWithCompany(
                    keyword,
                    jobStatus,
                    pageable);
        } else if (!keyword.isEmpty()) {
            jobs = jobRepository.findByTitleContainingIgnoreCaseWithCompany(
                    keyword,
                    pageable);
        } else if (!jobStatus.isEmpty()) {
            jobs = jobRepository.findByStatusWithCompany(
                    jobStatus,
                    pageable);
        } else {
            jobs = jobRepository.findAllWithCompany(pageable);
        }

        return jobs.map(this::mapToJobAdminDTO);
    }

    @Override
    @Transactional
    public Job updateJobStatus(Long jobId, String status) {
        String newStatus = status != null ? status.trim().toUpperCase() : "";

        if (!List.of("PENDING", "APPROVED", "REJECTED", "EXPIRED", "CLOSED").contains(newStatus)) {
            throw new RuntimeException("Trạng thái tin tuyển dụng không hợp lệ!");
        }

        Job job = jobRepository.findByIdWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Job với ID: " + jobId));

        if ("APPROVED".equals(newStatus)
                && job.getExpiredAt() != null
                && job.getExpiredAt().isBefore(java.time.LocalDate.now())) {
            job.setStatus("EXPIRED");
            return jobRepository.save(job);
        }

        job.setStatus(newStatus);

        return jobRepository.save(job);
    }

    @Override
    public Page<User> getAllUsers(int page, int size, String status, String search) {
        // Sắp xếp User mới đăng ký lên đầu
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String targetRole = "CANDIDATE"; // Cứng luôn chỉ lấy ứng viên

        // Xử lý chuỗi search
        String finalSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : "";

        // Xử lý Status String -> Enum
        com.jobconnect.entity.UserStatus finalStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                // Ép kiểu chuỗi (vd: "ACTIVE") thành Enum ACTIVE
                finalStatus = com.jobconnect.entity.UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                finalStatus = null;
            }
        }

        // Gọi hàm Query gom tất cả vào 1 phát ăn luôn
        return userRepository.findCandidatesWithFilter(targetRole, finalStatus, finalSearch, pageable);
    }

    @Override
    public List<UserCV> getCandidateCVs(Long userId) {
        // Kiểm tra xem ứng viên có tồn tại không
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên với ID: " + userId));

        // Query toàn bộ CV của ứng viên này trong DB
        return userCVRepository.findByUserId(userId);
    }

    @Override
    public User updateUserStatus(Long userId, boolean isActive) {
        // Tìm user theo ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        if (isActive) {
            user.setStatus(com.jobconnect.entity.UserStatus.ACTIVE); // Mở khóa
        } else {
            user.setStatus(com.jobconnect.entity.UserStatus.BANNED); // Khóa tài khoản
        }

        // Lưu lại vào DB và trả về
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public JobAdminDTO getJobById(Long id) {
        Job job = jobRepository.findByIdWithCompany(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin đăng với ID: " + id));

        return mapToJobAdminDTO(job);
    }

    private JobAdminDTO mapToJobAdminDTO(Job job) {
        Company company = job.getCompany();

        return JobAdminDTO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .location(job.getLocation())
                .salary(job.getSalary())
                .category(job.getCategory())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .employmentType(job.getEmploymentType())
                .createdAt(job.getCreatedAt())
                .expiredAt(job.getExpiredAt())
                .status(job.getStatus())
                .rejectionReason(job.getRejectionReason())
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getName() : "Không rõ công ty")
                .companyLogo(company != null ? company.getLogo() : null)
                .companyAddress(company != null ? company.getAddress() : null)
                .build();
    }

    @Override
    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty với ID: " + id));
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueStatsDTO getRevenueStats(String range) {
        try {
            log.info("Admin đang lấy thống kê doanh thu với bộ lọc: {}", range);

            LocalDateTime startDate = calculateStartDate(range);

            List<Transaction> transactions = transactionRepository.findByCreatedAtAfter(startDate);
            List<Transaction> recentTxns = transactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                    .getContent();

            // Tính Summary
            BigDecimal totalRevenue = BigDecimal.ZERO;
            long successCount = 0;

            for (Transaction txn : transactions) {
                if ("SUCCESS".equalsIgnoreCase(txn.getStatus()) && txn.getAmount() != null) {
                    totalRevenue = totalRevenue.add(txn.getAmount());
                    successCount++;
                }
            }

            double successRate = 0.0;
            if (!transactions.isEmpty()) {
                successRate = (double) successCount / transactions.size() * 100;
                successRate = Math.round(successRate * 100.0) / 100.0;
            }

            SummaryDTO summary = SummaryDTO.builder()
                    .totalRevenue(totalRevenue)
                    .totalTransactions(transactions.size())
                    .successRate(successRate)
                    .growth(12.5)
                    .build();

            // Tính Chart Data
            Map<String, BigDecimal> revenueByMonth = initEmptyChartData(range);
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("M");

            for (Transaction txn : transactions) {
                if ("SUCCESS".equalsIgnoreCase(txn.getStatus()) && txn.getAmount() != null) {
                    String monthLabel = "Tháng " + txn.getCreatedAt().format(monthFormatter);
                    if (revenueByMonth.containsKey(monthLabel)) {
                        revenueByMonth.put(monthLabel, revenueByMonth.get(monthLabel).add(txn.getAmount()));
                    }
                }
            }

            List<ChartDataDTO> chartData = revenueByMonth.entrySet().stream()
                    .map(entry -> new ChartDataDTO(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            // Xử lý Lịch sử giao dịch (Chống N+1)
            List<Long> employerIds = recentTxns.stream()
                    .map(Transaction::getEmployerId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, String> employerNames = new HashMap<>();
            if (!employerIds.isEmpty()) {
                // Dùng .putAll() để không làm mất tính effectively final của biến
                employerNames.putAll(
                        userRepository.findByIdIn(employerIds).stream()
                                .collect(Collectors.toMap(User::getId, User::getFullName,
                                        (existing, replacement) -> existing)));
            }

            Map<Long, String> packageNames = jobPackageRepository.findAll().stream()
                    .collect(Collectors.toMap(JobPackage::getId, JobPackage::getName));

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

            List<RecentTransactionDTO> recentTransactions = recentTxns.stream()
                    .map(txn -> RecentTransactionDTO.builder()
                            .id(txn.getTxnRef() != null ? txn.getTxnRef() : "N/A")
                            .company(employerNames.getOrDefault(txn.getEmployerId(), "Công ty không tồn tại"))
                            .packageType(packageNames.getOrDefault(txn.getPackageId(), "Gói bị xóa"))
                            .amount(txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO)
                            .date(txn.getCreatedAt() != null ? txn.getCreatedAt().format(dtf) : "Không xác định")
                            .status(txn.getStatus())
                            .build())
                    .collect(Collectors.toList());

            return RevenueStatsDTO.builder()
                    .summary(summary)
                    .chartData(chartData)
                    .recentTransactions(recentTransactions)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi hệ thống khi thống kê doanh thu: ", e);
            throw new RuntimeException("Không thể tải dữ liệu báo cáo doanh thu lúc này!");
        }
    }

    // --- CÁC HÀM ULTILITY PHỤ TRỢ ---
    private LocalDateTime calculateStartDate(String range) {
        LocalDateTime now = LocalDateTime.now();
        if (range == null)
            return now.minusMonths(6);

        switch (range) {
            case "7days":
                return now.minusDays(7);
            case "30days":
                return now.minusDays(30);
            case "6months":
                return now.minusMonths(6);
            case "this_year":
                return now.withDayOfYear(1).withHour(0).withMinute(0);
            default:
                return now.minusMonths(6);
        }
    }

    private Map<String, BigDecimal> initEmptyChartData(String range) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if ("6months".equals(range) || "this_year".equals(range)) {
            for (int i = 5; i >= 0; i--) {
                String monthLabel = "Tháng "
                        + LocalDateTime.now().minusMonths(i).format(DateTimeFormatter.ofPattern("M"));
                map.put(monthLabel, BigDecimal.ZERO);
            }
        }
        return map;
    }
}
