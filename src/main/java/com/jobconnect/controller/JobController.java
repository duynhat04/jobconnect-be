package com.jobconnect.controller;

import com.jobconnect.dto.JobRequest;
import com.jobconnect.dto.JobResponse;
import com.jobconnect.entity.EmploymentType;
import com.jobconnect.service.JobService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

    // =========================
    // PUBLIC API
    // =========================

    // Lấy danh sách job đã duyệt và chưa hết hạn
    // GET /api/jobs
    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        return ResponseEntity.ok(jobService.getApprovedJobs());
    }

    // Tìm kiếm, lọc job public
    // GET /api/jobs/search?keyword=java&location=hanoi&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minSalary,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<JobResponse> result = jobService.searchJobs(
                    keyword,
                    location,
                    category,
                    minSalary,
                    employmentType,
                    page,
                    size
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tìm kiếm: " + e.getMessage())
            );
        }
    }

    // Lấy danh sách category
    // GET /api/jobs/categories
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(jobService.getAllCategories());
    }

    // Lấy job public theo công ty
    // GET /api/jobs/company/{companyId}
    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getJobsByCompany(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        try {
            return ResponseEntity.ok(
                    jobService.getPublicJobsByCompany(companyId, page, size)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tải việc làm của công ty: " + e.getMessage())
            );
        }
    }

    // Lấy danh sách tin đã đăng của công ty đang đăng nhập
    // GET /api/jobs/my-jobs
    @GetMapping("/my-jobs")
    public ResponseEntity<?> getMyJobs() {
        try {
            String email = getCurrentUserIdentifier();

            return ResponseEntity.ok(jobService.getMyJobs(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi tải danh sách tin tuyển dụng: " + e.getMessage())
            );
        }
    }

    // Lấy job liên quan
    // GET /api/jobs/{id}/related
    @GetMapping("/{id}/related")
    public ResponseEntity<?> getRelatedJobs(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(jobService.getRelatedJobs(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // Lấy chi tiết một job
    // GET /api/jobs/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable Long id) {
        try {
            JobResponse job = jobService.getJobById(id);

            return ResponseEntity.ok(job);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // =========================
    // EMPLOYER API
    // =========================

    // Nhà tuyển dụng tạo bài đăng
    // POST /api/jobs
    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest jobRequest) {
        try {
            String email = getCurrentUserIdentifier();

            JobResponse createdJob = jobService.createJob(jobRequest, email);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdJob);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Lỗi hệ thống khi tạo tin tuyển dụng: " + e.getMessage())
            );
        }
    }

    // Nhà tuyển dụng cập nhật bài đăng
    // PUT /api/jobs/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest jobRequest
    ) {
        try {
            String email = getCurrentUserIdentifier();

            JobResponse updatedJob = jobService.updateJob(id, jobRequest, email);

            return ResponseEntity.ok(updatedJob);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Lỗi hệ thống khi cập nhật tin tuyển dụng: " + e.getMessage())
            );
        }
    }

    // Nhà tuyển dụng đóng tuyển
    // PUT /api/jobs/{id}/close
    @PutMapping("/{id}/close")
    public ResponseEntity<?> closeJob(@PathVariable Long id) {
        try {
            String email = getCurrentUserIdentifier();

            JobResponse closedJob = jobService.closeJob(id, email);

            return ResponseEntity.ok(closedJob);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Lỗi hệ thống khi đóng tin tuyển dụng: " + e.getMessage())
            );
        }
    }

    // Nhà tuyển dụng xóa bài đăng
    // DELETE /api/jobs/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Principal principal) {
        try {
            String email = principal != null ? principal.getName() : getCurrentUserIdentifier();

            jobService.deleteJob(id, email);

            return ResponseEntity.ok(
                    Map.of("message", "Xóa tin tuyển dụng thành công!")
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Có lỗi hệ thống xảy ra!")
            );
        }
    }

    // =========================
    // ADMIN API
    // =========================

    // Admin duyệt job
    // PUT /api/jobs/approve/{id}
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveJob(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(jobService.approveJob(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // Admin từ chối job
    // PUT /api/jobs/reject/{id}
    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectJob(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        try {
            String reason = body.get("reason");

            return ResponseEntity.ok(jobService.rejectJob(id, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // =========================
    // HELPER
    // =========================

    private String getCurrentUserIdentifier() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof com.jobconnect.entity.User) {
            return ((com.jobconnect.entity.User) principal).getEmail();
        }

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }

        return principal.toString();
    }
}