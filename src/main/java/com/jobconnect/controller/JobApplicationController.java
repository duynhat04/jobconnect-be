package com.jobconnect.controller;

import com.jobconnect.entity.JobApplication;
import com.jobconnect.service.JobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin("*")
public class JobApplicationController {

    @Autowired
    private JobApplicationService applicationService;

    // API nộp CV (Upload File)
    @PostMapping("/apply")
    public ResponseEntity<?> applyJob(
            @RequestParam("jobId") Long jobId,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam("cvFile") MultipartFile cvFile) {

        try {
            // 1. Lấy "cục" dữ liệu người dùng từ Token ra
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String email = "";

            // 2. Mở hộp lấy đúng Email (hoặc Username)
            if (principal instanceof com.jobconnect.entity.User) {
                // Ép kiểu về User và lấy email
                email = ((com.jobconnect.entity.User) principal).getEmail();

                // LƯU Ý: Nếu Entity User của bạn không có hàm getEmail() mà dùng getUsername()
                // thì bạn sửa lại thành .getUsername() ở dòng trên nhé!

            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else {
                email = principal.toString(); // Chống cháy
            }

            System.out.println(">>>> TÊN HOẶC EMAIL CHUẨN CHUẨN LÀ: " + email);

            // 3. Gọi Service xử lý
            JobApplication result = applicationService.applyJob(email, jobId, cvFile, coverLetter);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi nộp CV: " + e.getMessage());
        }
    }
    // Hàm phụ trợ: Lấy Email (hoặc Username) chuẩn từ Token của người đang đăng nhập
    private String getCurrentUserIdentifier() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.jobconnect.entity.User) {
            return ((com.jobconnect.entity.User) principal).getEmail(); // Hoặc getUsername() tùy entity của bạn
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    // 1. API Ứng viên xem danh sách các Job mình đã nộp
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications() {
        try {
            String email = getCurrentUserIdentifier();
            return ResponseEntity.ok(applicationService.getMyApplications(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 2. API Nhà tuyển dụng xem danh sách CV nộp vào 1 Job cụ thể
    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getApplicationsForJob(@PathVariable Long jobId) {
        try {
            String email = getCurrentUserIdentifier();
            return ResponseEntity.ok(applicationService.getApplicationsForJob(jobId, email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 3. API Nhà tuyển dụng duyệt/từ chối 1 CV
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable("id") Long applicationId,
            @RequestParam("status") String status) {
        try {
            String email = getCurrentUserIdentifier();
            return ResponseEntity.ok(applicationService.updateApplicationStatus(applicationId, status, email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // Thêm API này để Nhà tuyển dụng lấy TẤT CẢ CV của công ty mình
    @GetMapping("/employer/all")
    public ResponseEntity<?> getAllCandidatesForMyCompany() {
        try {
            String email = getCurrentUserIdentifier();
            // Lưu ý: Đảm bảo bạn đã thêm hàm getAllApplicationsForMyCompany() vào JobApplicationService như tui hướng dẫn lúc nãy nhé
            return ResponseEntity.ok(applicationService.getAllApplicationsForMyCompany(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}