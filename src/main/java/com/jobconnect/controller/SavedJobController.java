package com.jobconnect.controller;

import com.jobconnect.service.SavedJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/saved-jobs")
@CrossOrigin("*")
public class SavedJobController {

    @Autowired
    private SavedJobService savedJobService;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public ResponseEntity<?> getMySavedJobs() {
        return ResponseEntity.ok(savedJobService.getMySavedJobs(getCurrentUserEmail()));
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<?> saveJob(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(savedJobService.saveJob(getCurrentUserEmail(), jobId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> unsaveJob(@PathVariable Long jobId) {
        try {
            savedJobService.unsaveJob(getCurrentUserEmail(), jobId);
            return ResponseEntity.ok("Đã bỏ lưu công việc!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}