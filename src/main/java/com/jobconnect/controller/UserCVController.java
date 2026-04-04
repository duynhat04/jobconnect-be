package com.jobconnect.controller;

import com.jobconnect.service.UserCVService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cv")
@CrossOrigin("*") // Chỗ này để nguyên cũng được vì ông đã config CORS ở SecurityConfig rồi
public class UserCVController {

    @Autowired
    private UserCVService userCVService;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public ResponseEntity<?> getMyCVs() {
        try {
            return ResponseEntity.ok(userCVService.getMyCVs(getCurrentUserEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCV(@RequestParam("file") MultipartFile file,
                                      @RequestParam("cvName") String cvName) {
        try {
            return ResponseEntity.ok(userCVService.uploadCV(getCurrentUserEmail(), file, cvName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi upload CV: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCV(@PathVariable Long id) {
        try {
            userCVService.deleteCV(id, getCurrentUserEmail());
            return ResponseEntity.ok("Đã xóa CV thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi xóa: " + e.getMessage());
        }
    }
}