package com.jobconnect.controller;

import com.jobconnect.dto.JobRequest;
import com.jobconnect.entity.Job;
import com.jobconnect.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin("*")
public class JobController {

    @Autowired
    private JobService jobService;

    // --- API CHO ỨNG VIÊN (PUBLIC) ---

    // Chỉ hiển thị các Job ĐÃ ĐƯỢC DUYỆT
    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getApprovedJobs());
    }

    // --- API CHO NHÀ TUYỂN DỤNG ---

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody JobRequest jobRequest) {
        try {
            // Lấy email từ hàm phụ trợ getCurrentUserIdentifier() của sếp
            String email = getCurrentUserIdentifier();
            Job createdJob = jobService.createJob(jobRequest, email);
            return ResponseEntity.ok(createdJob);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody JobRequest jobRequest) {
        try {
            // Lấy email người đang đăng nhập để Service kiểm tra quyền (Chỉ owner mới được
            // sửa)
            String email = getCurrentUserIdentifier();

            // Gọi Service để update
            Job updatedJob = jobService.updateJob(id, jobRequest, email);

            return ResponseEntity.ok(updatedJob);
        } catch (RuntimeException e) {
            // Trả về lỗi 403 (Forbidden) hoặc 400 (Bad Request) tùy logic
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Principal principal) {
        try {
            // principal.getName() sẽ lấy ra email/username của người đang đăng nhập từ
            // Token
            String email = principal.getName();

            // Gọi Service để xử lý logic xóa
            jobService.deleteJob(id, email);

            return ResponseEntity.ok("Xóa tin tuyển dụng thành công!");
        } catch (RuntimeException e) {
            // Nếu lỗi (không tìm thấy job, hoặc xóa trộm job của công ty khác) thì báo lỗi
            // 400
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Có lỗi hệ thống xảy ra!");
        }
    }

    // Hàm phụ trợ: Lấy Email/Username từ Token
    private String getCurrentUserIdentifier() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (principal instanceof com.jobconnect.entity.User) {
            return ((com.jobconnect.entity.User) principal).getEmail(); // Hoặc getUsername()
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(jobService.getAllCategories());
    }

    // API Lấy danh sách tin đã đăng của riêng Công ty đang đăng nhập
    @GetMapping("/my-jobs")
    public ResponseEntity<?> getMyJobs() {
        try {
            String email = getCurrentUserIdentifier();
            return ResponseEntity.ok(jobService.getMyJobs(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
    // --- API CHO ADMIN ---

    // Admin duyệt Job
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveJob(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(jobService.approveJob(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin từ chối Job (Truyền lý do qua Body dưới dạng JSON)
    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectJob(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            return ResponseEntity.ok(jobService.rejectJob(id, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API Tìm kiếm và Lọc Job (Ai cũng có thể xem, không cần Token)
    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minSalary,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // Truyền category vào service
            return ResponseEntity.ok(jobService.searchJobs(keyword, location, category, minSalary, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tìm kiếm: " + e.getMessage());
        }
    }

    // API lấy chi tiết 1 Job theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable Long id) {
        try {
            Job job = jobService.getJobById(id);
            return ResponseEntity.ok(job); // Trả về data Job
        } catch (RuntimeException e) {
            // Nếu không tìm thấy, trả về thông báo lỗi
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<?> getRelatedJobs(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(jobService.getRelatedJobs(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}