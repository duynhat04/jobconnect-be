package com.jobconnect.controller;

import com.jobconnect.dto.CompanyListResponse;
import com.jobconnect.dto.CompanyStatsDTO;
import com.jobconnect.entity.Company;
import com.jobconnect.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin("*")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    // =========================
    // PUBLIC: Người dùng xem danh sách công ty
    // GET /api/companies?page=0&size=9
    // GET /api/companies?keyword=fpt&page=0&size=9
    // =========================
    @GetMapping
    public ResponseEntity<?> getPublicCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        try {
            Page<CompanyListResponse> companies =
                    companyService.getPublicCompanies(keyword, page, size);

            return ResponseEntity.ok(companies);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tải danh sách công ty: " + e.getMessage())
            );
        }
    }

    // =========================
    // PUBLIC: Người dùng xem chi tiết công ty
    // GET /api/companies/{id}
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicCompanyDetail(@PathVariable Long id) {
        try {
            CompanyListResponse company = companyService.getPublicCompanyDetail(id);

            return ResponseEntity.ok(company);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tải thông tin công ty: " + e.getMessage())
            );
        }
    }

    // =========================
    // EMPLOYER: Đăng ký công ty
    // POST /api/companies/register/{userId}
    // =========================
    @PostMapping("/register/{userId}")
    public ResponseEntity<?> registerCompany(
            @PathVariable Long userId,
            @RequestBody Company company
    ) {
        try {
            Company savedCompany = companyService.registerCompany(userId, company);

            return ResponseEntity.ok(savedCompany);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // =========================
    // EMPLOYER: Lấy công ty của tài khoản đang đăng nhập
    // GET /api/companies/my-company
    // =========================
    @GetMapping("/my-company")
    public ResponseEntity<?> getMyCompany() {
        try {
            Company company = companyService.getMyCompany(getCurrentUserEmail());

            return ResponseEntity.ok(company);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // =========================
    // EMPLOYER: Thống kê dashboard
    // GET /api/companies/my-stats
    // =========================
    @GetMapping("/my-stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            CompanyStatsDTO stats = companyService.getCompanyStats(getCurrentUserEmail());

            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // =========================
    // EMPLOYER: Cập nhật hồ sơ công ty
    // PUT /api/companies/my-profile
    // =========================
    @PutMapping("/my-profile")
    public ResponseEntity<?> updateMyProfile(@RequestBody Company updatedData) {
        try {
            Company savedCompany = companyService.updateMyCompany(
                    getCurrentUserEmail(),
                    updatedData
            );

            return ResponseEntity.ok(savedCompany);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // =========================
    // HELPER
    // =========================
    private String getCurrentUserEmail() {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }
}