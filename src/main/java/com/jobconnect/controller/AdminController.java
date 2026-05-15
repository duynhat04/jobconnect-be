package com.jobconnect.controller;

import com.jobconnect.dto.CompanyAdminDTO;
import com.jobconnect.dto.DashboardStatsDTO;
import com.jobconnect.dto.RevenueStatsDTO;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import com.jobconnect.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO data = adminService.getDashboardStats();
        return ResponseEntity.ok(data);
    }

    // LẤY DANH SÁCH CÔNG TY (PHÂN TRANG)
    @GetMapping("/companies")
    public ResponseEntity<Page<CompanyAdminDTO>> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Page<CompanyAdminDTO> companies = adminService.getAllCompanies(page, size, status, search);

        return ResponseEntity.ok(companies);
    }

    // DUYỆT / TỪ CHỐI CÔNG TY
    @PutMapping("/companies/{id}/status")
    public ResponseEntity<Company> updateCompanyStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        Company updatedCompany = adminService.updateCompanyStatus(id, status);
        return ResponseEntity.ok(updatedCompany);
    }

    // Lấy danh sách
    @GetMapping("/jobs")
    public ResponseEntity<Page<Job>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(adminService.getAllJobs(page, size, status, search));
    }

    // Duyệt / Khóa Job
    @PutMapping("/jobs/{id}/status")
    public ResponseEntity<Job> updateJobStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        return ResponseEntity.ok(adminService.updateJobStatus(id, status));
    }

    // LẤY DANH SÁCH CV CỦA 1 ỨNG VIÊN CỤ THỂ (Để Admin click vào user thì hiện ra
    // CV)
    @GetMapping("/users/{id}/cvs")
    public ResponseEntity<?> getCandidateCVs(@PathVariable Long id) {
        // Mày có thể gọi thẳng UserCVService ở đây, hoặc viết thêm hàm getCandidateCVs
        // trong AdminService
        return ResponseEntity.ok(adminService.getCandidateCVs(id));
    }

    // KHÓA / MỞ KHÓA TÀI KHOẢN ỨNG VIÊN
    @PutMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) { // Hoặc dùng String status tùy design DB của m
        return ResponseEntity.ok(adminService.updateUserStatus(id, isActive));
    }

    // LẤY CHI TIẾT 1 JOB
    @GetMapping("/jobs/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        Job job = adminService.getJobById(id);
        return ResponseEntity.ok(job);
    }

    // LẤY CHI TIẾT 1 CÔNG TY
    @GetMapping("/companies/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable Long id) {
        Company company = adminService.getCompanyById(id);
        return ResponseEntity.ok(company);
    }

    // LẤY CHI TIẾT 1 ỨNG VIÊN (USER)
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // Giả sử sếp đã viết hàm getUserById trong AdminService.
        // Nếu chưa có thì sếp sang AdminService viết thêm hàm findById nhé!
        User user = adminService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // Lấy danh sách User
    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(adminService.getAllUsers(page, size, status, search));
    }

    // LẤY BÁO CÁO & THỐNG KÊ DOANH THU
    @GetMapping("/revenue-stats")
    public ResponseEntity<RevenueStatsDTO> getRevenueStats(
            @RequestParam(required = false, defaultValue = "6months") String range) {

        RevenueStatsDTO stats = adminService.getRevenueStats(range);
        return ResponseEntity.ok(stats);
    }
}