package com.jobconnect.service;

import com.jobconnect.dto.DashboardStatsDTO;
import com.jobconnect.dto.RevenueStatsDTO;
import com.jobconnect.dto.CompanyAdminDTO;
import com.jobconnect.dto.JobAdminDTO;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import com.jobconnect.entity.UserCV;
import java.util.List;

import org.springframework.data.domain.Page;

public interface AdminService {

    DashboardStatsDTO getDashboardStats();

    RevenueStatsDTO getRevenueStats(String range);
    // Lấy danh sách công ty (Có phân trang & Lọc trạng thái)
    Page<CompanyAdminDTO> getAllCompanies(int page, int size, String status, String search);

    // Thay đổi trạng thái công ty (Duyệt / Từ chối)
    CompanyAdminDTO updateCompanyStatus(Long companyId, String status);

    // Lấy danh sách Job (Có phân trang)
    Page<JobAdminDTO> getAllJobs(int page, int size, String status, String search);

    // Cập nhật trạng thái Job (Duyệt / Từ chối / Khóa)
    Job updateJobStatus(Long jobId, String status);

    // LẤY DANH SÁCH USER
    Page<User> getAllUsers(int page, int size, String status, String search);

    // Lấy danh sách CV của ứng viên cho Admin xem
    List<UserCV> getCandidateCVs(Long userId);

    // Khóa / Mở khóa tài khoản ứng viên (Ban/Unban)
    User updateUserStatus(Long userId, boolean isActive);

    JobAdminDTO getJobById(Long id);

    // Lấy chi tiết 1 công ty
    Company getCompanyById(Long id);

    // LẤY CHI TIẾT 1 ỨNG VIÊN
    User getUserById(Long id);


}