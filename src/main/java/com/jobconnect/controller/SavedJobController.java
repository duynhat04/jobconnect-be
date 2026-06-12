package com.jobconnect.controller;

import com.jobconnect.dto.SavedJobResponse;
import com.jobconnect.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/saved-jobs")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SavedJobController {

    private final SavedJobService savedJobService;

    @GetMapping
    public ResponseEntity<List<SavedJobResponse>> getMySavedJobs(Principal principal) {
        return ResponseEntity.ok(
                savedJobService.getMySavedJobs(principal.getName())
        );
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<?> saveJob(
            @PathVariable Long jobId,
            Principal principal
    ) {
        try {
            SavedJobResponse response = savedJobService.saveJob(
                    principal.getName(),
                    jobId
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> unsaveJob(
            @PathVariable Long jobId,
            Principal principal
    ) {
        try {
            savedJobService.unsaveJob(principal.getName(), jobId);

            return ResponseEntity.ok(
                    Map.of("message", "Đã bỏ lưu công việc!")
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Boolean>> checkSavedStatus(
            @PathVariable Long jobId,
            Principal principal
    ) {
        boolean saved = savedJobService.isJobSaved(principal.getName(), jobId);

        return ResponseEntity.ok(
                Map.of("saved", saved)
        );
    }
}