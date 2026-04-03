package com.jobconnect.controller;

import com.jobconnect.entity.Company;
import com.jobconnect.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin("*")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    // Truyền userId trực tiếp trên thanh URL để biết ai là người đang xin đăng ký
    @PostMapping("/register/{userId}")
    public ResponseEntity<?> registerCompany(@PathVariable Long userId, @RequestBody Company company) {
        try {
            Company savedCompany = companyService.registerCompany(userId, company);
            // Nếu thành công -> Trả về mã 200 OK và thông tin Công ty
            return ResponseEntity.ok(savedCompany);
        } catch (RuntimeException e) {
            // Nếu dính lỗi ở Service -> Trả về mã 400 Bad Request và câu thông báo lỗi
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // API để Admin duyệt công ty
    @PutMapping("/approve/{companyId}")
    public ResponseEntity<?> approveCompany(@PathVariable Long companyId) {
        try {
            Company approvedCompany = companyService.approveCompany(companyId);
            return ResponseEntity.ok(approvedCompany);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Thêm hàm phụ trợ lấy Email tương tự như bên JobController
    private String getCurrentUserIdentifier() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.jobconnect.entity.User) {
            return ((com.jobconnect.entity.User) principal).getEmail();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    // API Lấy thông tin Công ty của Nhà tuyển dụng đang đăng nhập
    @GetMapping("/my-company")
    public ResponseEntity<?> getMyCompany() {
        try {
            String email = getCurrentUserIdentifier();
            return ResponseEntity.ok(companyService.getMyCompany(email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // API Lấy thống kê cho Dashboard
    @GetMapping("/my-stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            String email = getCurrentUserIdentifier(); // Hàm này đã có sẵn trong CompanyController của bạn
            Map<String, Object> stats = companyService.getCompanyStats(email);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}