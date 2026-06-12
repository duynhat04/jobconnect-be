package com.jobconnect.controller;

import com.jobconnect.dto.CompanyListResponse;
import com.jobconnect.dto.CompanyRequest;
import com.jobconnect.dto.CompanyResponse;
import com.jobconnect.dto.CompanyStatsDTO;
import com.jobconnect.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin("*")
public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    @Autowired
    private CompanyService companyService;

    // =========================
    // PUBLIC: Danh sách công ty
    // GET /api/companies?page=0&size=9
    // GET /api/companies?keyword=fpt&page=0&size=9
    // =========================
    @GetMapping
    public ResponseEntity<?> getPublicCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        try {
            Page<CompanyListResponse> companies = companyService.getPublicCompanies(keyword, page, size);

            return ResponseEntity.ok(companies);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi tải danh sách công ty", e);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tải danh sách công ty"));
        }
    }

    // =========================
    // PUBLIC: Chi tiết công ty
    // GET /api/companies/{id}
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicCompanyDetail(@PathVariable Long id) {
        try {
            CompanyListResponse company = companyService.getPublicCompanyDetail(id);

            return ResponseEntity.ok(company);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi tải chi tiết công ty id={}", id, e);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tải thông tin công ty"));
        }
    }

    // =========================
    // EMPLOYER: Đăng ký công ty
    // POST /api/companies/register/{userId}
    // =========================
    @PostMapping("/register/{userId}")
    public ResponseEntity<?> registerCompany(
            @PathVariable Long userId,
            @RequestBody CompanyRequest request) {
        try {
            CompanyResponse savedCompany = companyService.registerCompany(userId, request);

            return ResponseEntity.ok(savedCompany);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi đăng ký công ty userId={}", userId, e);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi đăng ký công ty"));
        }
    }

    // =========================
    // EMPLOYER: Lấy công ty của tài khoản đang đăng nhập
    // GET /api/companies/my-company
    // =========================
    @GetMapping("/my-company")
    public ResponseEntity<?> getMyCompany() {
        try {
            String email = getCurrentUserEmail();
            CompanyResponse company = companyService.getMyCompany(email);

            return ResponseEntity.ok(company);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi lấy công ty của tài khoản đang đăng nhập", e);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tải thông tin công ty"));
        }
    }

    // =========================
    // EMPLOYER: Thống kê dashboard
    // GET /api/companies/my-stats
    // =========================
    @GetMapping("/my-stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            String email = getCurrentUserEmail();
            CompanyStatsDTO stats = companyService.getCompanyStats(email);

            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi tải thống kê dashboard công ty", e);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tải thống kê dashboard"));
        }
    }

    // =========================
    // EMPLOYER: Cập nhật hồ sơ công ty
    // PUT /api/companies/my-profile
    // =========================
    @PutMapping("/my-profile")
    public ResponseEntity<?> updateMyProfile(@RequestBody CompanyRequest request) {
        try {
            String email = getCurrentUserEmail();
            CompanyResponse savedCompany = companyService.updateMyCompany(email, request);

            return ResponseEntity.ok(savedCompany);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi cập nhật hồ sơ công ty", e);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi cập nhật hồ sơ công ty"));
        }
    }

    // =========================
    // HELPER
    // =========================
    private String getCurrentUserEmail() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng nhập!");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof com.jobconnect.entity.User) {
            return ((com.jobconnect.entity.User) principal).getEmail();
        }

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }

        return authentication.getName();
    }

    @PostMapping(value = "/my-profile/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyCompanyLogo(@RequestParam("file") MultipartFile file) {
        try {
            String email = getCurrentUserEmail();
            CompanyResponse company = companyService.uploadMyCompanyLogo(email, file);

            return ResponseEntity.ok(company);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi upload logo công ty", e);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi upload logo công ty"));
        }
    }

    @PostMapping(value = "/my-profile/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyCompanyCoverImage(@RequestParam("file") MultipartFile file) {
        try {
            String email = getCurrentUserEmail();
            CompanyResponse company = companyService.uploadMyCompanyCoverImage(email, file);

            return ResponseEntity.ok(company);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi upload ảnh bìa công ty", e);
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi upload ảnh bìa công ty"));
        }
    }
}