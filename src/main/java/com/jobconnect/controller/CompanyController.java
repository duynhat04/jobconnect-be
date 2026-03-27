package com.jobconnect.controller;

import com.jobconnect.entity.Company;
import com.jobconnect.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}