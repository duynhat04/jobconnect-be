package com.jobconnect.service;

import com.jobconnect.dto.DashboardStatsDTO;
import com.jobconnect.dto.RecentCompanyDTO;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import com.jobconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;

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
    public Page<Company> getAllCompanies(int page, int size, String status, String search) {
        // Sắp xếp ID giảm dần (mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // Nếu có nhập từ khóa tìm kiếm
        if (search != null && !search.isEmpty()) {
            return companyRepository.findByNameContainingIgnoreCase(search, pageable);
        }

        // Nếu có chọn bộ lọc trạng thái (PENDING, APPROVED...)
        if (status != null && !status.isEmpty()) {
            return companyRepository.findByStatus(status, pageable);
        }

        // Nếu không lọc gì cả thì lấy tất cả
        return companyRepository.findAll(pageable);
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
                 user.setRole("USER");
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
    public Page<User> getAllUsers(int page, int size, String search) {
        // Sắp xếp User mới đăng ký lên đầu
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // Nếu Admin có nhập từ khóa tìm kiếm vào ô Search
        if (search != null && !search.isEmpty()) {
            return userRepository.findByEmailContainingIgnoreCase(search, pageable);
        }

        // Nếu không tìm kiếm gì thì trả về tất cả
        return userRepository.findAll(pageable);
    }
}