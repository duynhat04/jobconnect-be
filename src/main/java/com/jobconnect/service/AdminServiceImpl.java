package com.jobconnect.service;

import com.jobconnect.dto.DashboardStatsDTO;
import com.jobconnect.dto.RecentCompanyDTO;
import com.jobconnect.dto.RevenueStatsDTO;
import com.jobconnect.dto.SummaryDTO;
import com.jobconnect.dto.ChartDataDTO;
import com.jobconnect.dto.RecentTransactionDTO;
import com.jobconnect.dto.CompanyAdminDTO;

import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobPackage;
import com.jobconnect.entity.User;
import com.jobconnect.entity.UserCV;
import com.jobconnect.entity.Transaction; 

import com.jobconnect.repository.*;
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
        public DashboardStatsDTO getDashboardStats() {
            DashboardStatsDTO statsDTO = new DashboardStatsDTO();

            // 1. Đếm số lượng
            statsDTO.setTotalUsers(userRepository.count());
            statsDTO.setTotalCompanies(companyRepository.count());
            statsDTO.setTotalJobs(jobRepository.count());
            statsDTO.setTotalApplications(jobApplicationRepository.count());

            // 2. Lấy 5 công ty mới nhất theo ID giảm dần
            List<RecentCompanyDTO> recentCompanies = companyRepository.findTop5ByOrderByIdDesc()
                    .stream()
                    .map(company -> {
                        RecentCompanyDTO dto = new RecentCompanyDTO();
                        dto.setName(company.getName());

                        // Đã fix: Lấy email từ User đại diện
                        if (company.getUser() != null) {
                            dto.setEmail(company.getUser().getEmail());
                        } else {
                            dto.setEmail("Không có email");
                        }

                        dto.setStatus(company.getStatus());
                        return dto;
                    }).collect(Collectors.toList());

            statsDTO.setRecentCompanies(recentCompanies);

            return statsDTO;
        }

        @Override
        public Page<CompanyAdminDTO> getAllCompanies(int page, int size, String status, String search) {
            // Sắp xếp ID giảm dần (mới nhất lên đầu)
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

            // Nếu có nhập từ khóa tìm kiếm
            if (search != null && !search.isEmpty()) {
                return companyRepository.findByNameContainingIgnoreCase(search, pageable)
                        .map(company -> {
                            CompanyAdminDTO dto = new CompanyAdminDTO();
                            dto.setId(company.getId());
                            dto.setName(company.getName());
                            dto.setTaxCode(company.getTaxCode());
                            dto.setAddress(company.getAddress());
                            dto.setWebsite(company.getWebsite());
                            dto.setPhone(company.getPhone());
                            dto.setLogo(company.getLogo());
                            dto.setStatus(company.getStatus());
                            // Đã fix: Lấy email từ User đại diện
                            if (company.getUser() != null) {
                                dto.setEmail(company.getUser().getEmail());
                            } else {
                                dto.setEmail("Không có email");
                            }
                            dto.setCreatedAt(company.getCreatedAt() != null ? company.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
                            dto.setRemainingPosts(company.getRemainingPosts());
                            return dto;
                        });
            }

            // Nếu có chọn bộ lọc trạng thái (PENDING, APPROVED...)
            if (status != null && !status.isEmpty()) {
                return companyRepository.findByStatus(status, pageable)
                        .map(company -> {
                            CompanyAdminDTO dto = new CompanyAdminDTO();
                            dto.setId(company.getId());
                            dto.setName(company.getName());
                            dto.setTaxCode(company.getTaxCode());
                            dto.setAddress(company.getAddress());
                            dto.setWebsite(company.getWebsite());
                            dto.setPhone(company.getPhone());
                            dto.setLogo(company.getLogo());
                            dto.setStatus(company.getStatus());
                            // Đã fix: Lấy email từ User đại diện
                            if (company.getUser() != null) {
                                dto.setEmail(company.getUser().getEmail());
                            } else {
                                dto.setEmail("Không có email");
                            }
                            dto.setCreatedAt(company.getCreatedAt() != null ? company.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
                            dto.setRemainingPosts(company.getRemainingPosts());
                            return(dto);
                        });
            }

            // Nếu không lọc gì cả thì lấy tất cả
            return companyRepository.findAll(pageable).map(company -> {
                CompanyAdminDTO dto = new CompanyAdminDTO();
                dto.setId(company.getId());
                dto.setName(company.getName());
                dto.setTaxCode(company.getTaxCode());
                dto.setAddress(company.getAddress());
                dto.setWebsite(company.getWebsite());
                dto.setPhone(company.getPhone());
                dto.setLogo(company.getLogo());
                dto.setStatus(company.getStatus());
                // Đã fix: Lấy email từ User đại diện
                if (company.getUser() != null) {
                    dto.setEmail(company.getUser().getEmail());
                } else {
                    dto.setEmail("Không có email");
                }
                dto.setCreatedAt(company.getCreatedAt() != null ? company.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
                dto.setRemainingPosts(company.getRemainingPosts());
                return dto;
            });
        }

        @Override
        public Company updateCompanyStatus(Long companyId, String status) {
            // Tìm công ty xem có tồn tại không
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty với ID: " + companyId));

            // Cập nhật trạng thái mới cho Company
            company.setStatus(status);

            // Nếu Admin bấm "Duyệt" (truyền lên APPROVED)
            if ("APPROVED".equalsIgnoreCase(status)) {
                User user = company.getUser();
                // Nâng cấp user lên Nhà tuyển dụng nếu họ chưa phải là EMPLOYER
                if (user != null && !"EMPLOYER".equals(user.getRole())) {
                    user.setRole("EMPLOYER");
                    userRepository.save(user); 
                }
            } 
            // Khi Admin lỡ tay duyệt rồi nhưng sau đó đổi ý thành REJECTED 
            else if ("REJECTED".equalsIgnoreCase(status)) {
                User user = company.getUser();
                if (user != null && "EMPLOYER".equals(user.getRole())) {
                    user.setRole("CANDIDATE");
                    userRepository.save(user);
                }
            }

            // Lưu lại vào Database
            return companyRepository.save(company);
        }

        @Override
        public Page<Job> getAllJobs(int page, int size, String status, String search) {
            // Sắp xếp Job mới nhất lên đầu
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

            if (search != null && !search.isEmpty()) {
                return jobRepository.findByTitleContainingIgnoreCase(search, pageable);
            }

            if (status != null && !status.isEmpty()) {
                return jobRepository.findByStatus(status, pageable);
            }

            return jobRepository.findAll(pageable);
        }

        @Override
        public Job updateJobStatus(Long jobId, String status) {
            // Tìm Job trong DB
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Job với ID: " + jobId));

            // Cập nhật trạng thái mới
            job.setStatus(status);

            // Lưu lại
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
        public Job getJobById(Long id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tin đăng với ID: " + id));
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
            List<Transaction> recentTxns = transactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10)).getContent();

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
                        .collect(Collectors.toMap(User::getId, User::getFullName, (existing, replacement) -> existing))
                );
            }

            Map<Long, String> packageNames = jobPackageRepository.findAll().stream()
                    .collect(Collectors.toMap(JobPackage::getId, JobPackage::getName));

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            
            List<RecentTransactionDTO> recentTransactions = recentTxns.stream().map(txn -> 
                RecentTransactionDTO.builder()
                        .id(txn.getTxnRef() != null ? txn.getTxnRef() : "N/A") 
                        .company(employerNames.getOrDefault(txn.getEmployerId(), "Công ty không tồn tại"))
                        .packageType(packageNames.getOrDefault(txn.getPackageId(), "Gói bị xóa"))
                        .amount(txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO)
                        .date(txn.getCreatedAt() != null ? txn.getCreatedAt().format(dtf) : "Không xác định")
                        .status(txn.getStatus())
                        .build()
            ).collect(Collectors.toList());

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
        if (range == null) return now.minusMonths(6);
        
        switch (range) {
            case "7days": return now.minusDays(7);
            case "30days": return now.minusDays(30);
            case "6months": return now.minusMonths(6);
            case "this_year": return now.withDayOfYear(1).withHour(0).withMinute(0);
            default: return now.minusMonths(6);
        }
    }

    private Map<String, BigDecimal> initEmptyChartData(String range) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if ("6months".equals(range) || "this_year".equals(range)) {
            for (int i = 5; i >= 0; i--) {
                String monthLabel = "Tháng " + LocalDateTime.now().minusMonths(i).format(DateTimeFormatter.ofPattern("M"));
                map.put(monthLabel, BigDecimal.ZERO);
            }
        }
        return map;
    }
}
