package com.jobconnect.controller;

import com.jobconnect.dto.ApplyExistingDto;
import com.jobconnect.dto.ApplicationCandidateDto;
import com.jobconnect.dto.ApplicationResponse;
import com.jobconnect.service.JobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    @Autowired
    private JobApplicationService applicationService;

    // Nộp CV bằng file mới
    @PostMapping("/apply")
    public ResponseEntity<?> applyJob(
            @RequestParam("jobId") Long jobId,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam("cvFile") MultipartFile cvFile
    ) {
        try {
            String email = getCurrentUserIdentifier();

            ApplicationResponse result = applicationService.applyJob(
                    email,
                    jobId,
                    cvFile,
                    coverLetter
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi khi nộp CV: " + e.getMessage())
            );
        }
    }

    // Nộp CV bằng file mới - giữ endpoint cũ đang dùng
    @PostMapping("/apply-new")
    public ResponseEntity<?> applyNew(
            @RequestParam("jobId") Long jobId,
            @RequestParam("cvFile") MultipartFile cvFile,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            Principal principal
    ) {
        try {
            String email = principal != null ? principal.getName() : getCurrentUserIdentifier();

            ApplicationResponse app = applicationService.applyWithNewCV(
                    email,
                    jobId,
                    cvFile,
                    coverLetter
            );

            return ResponseEntity.ok(app);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // Nộp bằng CV đã lưu
    @PostMapping("/apply-existing")
    public ResponseEntity<?> applyExisting(
            @RequestParam("jobId") Long jobId,
            @RequestBody ApplyExistingDto request,
            Principal principal
    ) {
        try {
            String email = principal != null ? principal.getName() : getCurrentUserIdentifier();

            ApplicationResponse app = applicationService.applyWithExistingCV(
                    email,
                    jobId,
                    request.getUserCvId(),
                    request.getCoverLetter()
            );

            return ResponseEntity.ok(app);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // Ứng viên xem hồ sơ đã ứng tuyển
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications() {
        try {
            String email = getCurrentUserIdentifier();

            List<ApplicationResponse> result = applicationService.getMyApplications(email);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi: " + e.getMessage())
            );
        }
    }

    // NTD xem danh sách CV của 1 job
    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getApplicationsForJob(@PathVariable Long jobId) {
        try {
            String email = getCurrentUserIdentifier();

            List<ApplicationResponse> result = applicationService.getApplicationsForJob(jobId, email);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi: " + e.getMessage())
            );
        }
    }

    // NTD duyệt/từ chối CV
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable("id") Long applicationId,
            @RequestParam("status") String status
    ) {
        try {
            String email = getCurrentUserIdentifier();

            ApplicationResponse result = applicationService.updateApplicationStatus(
                    applicationId,
                    status,
                    email
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi: " + e.getMessage())
            );
        }
    }

    // NTD xem tất cả CV của công ty mình
    @GetMapping("/employer/all")
    public ResponseEntity<?> getAllCandidatesForMyCompany() {
        try {
            String email = getCurrentUserIdentifier();

            List<ApplicationResponse> result = applicationService.getAllApplicationsForMyCompany(email);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi: " + e.getMessage())
            );
        }
    }

    // NTD xem ứng viên theo từng job, có tìm kiếm/lọc/chấm điểm
    @GetMapping("/job/{jobId}/candidates")
    public ResponseEntity<?> searchCandidatesForJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            String email = getCurrentUserIdentifier();

            Page<ApplicationCandidateDto> result = applicationService.searchCandidatesForJob(
                    jobId,
                    email,
                    keyword,
                    status,
                    page,
                    size
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    private String getCurrentUserIdentifier() {
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
}