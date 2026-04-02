package com.jobconnect.service;

import com.jobconnect.dto.DashboardStatsDTO;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import org.springframework.data.domain.Page;

public interface AdminService {

    DashboardStatsDTO getDashboardStats();

    // Lấy danh sách công ty (Có phân trang & Lọc trạng thái)
    Page<Company> getAllCompanies(int page, int size, String status, String search);

    // Thay đổi trạng thái công ty (Duyệt / Từ chối)
    Company updateCompanyStatus(Long companyId, String status);

    // Lấy danh sách Job (Có phân trang)
    Page<Job> getAllJobs(int page, int size, String status, String search);

    // Cập nhật trạng thái Job (Duyệt / Từ chối / Khóa)
    Job updateJobStatus(Long jobId, String status);

    // Lấy danh sách Ứng viên / User (Có phân trang)
    Page<User> getAllUsers(int page, int size, String search);
}