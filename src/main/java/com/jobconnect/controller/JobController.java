package com.jobconnect.controller;

import com.jobconnect.dto.JobRequest;
import com.jobconnect.entity.Job;
import com.jobconnect.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.ok(jobService.createJob(jobRequest));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
            @RequestParam(required = false) Long minSalary,
            @RequestParam(defaultValue = "0") int page,  // Mặc định là trang 0 (trang đầu tiên)
            @RequestParam(defaultValue = "10") int size) { // Mặc định mỗi trang 10 bài
        try {
            return ResponseEntity.ok(jobService.searchJobs(keyword, location, minSalary, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tìm kiếm: " + e.getMessage());
        }
    }
}