package com.jobconnect.controller;

import com.jobconnect.dto.ApiResponse;
import com.jobconnect.dto.UserCVResponse;
import com.jobconnect.entity.UserCV;
import com.jobconnect.service.UserCVService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/cv")
public class UserCVController {

    @Autowired
    private UserCVService userCVService;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserCVResponse>>> getMyCVs() {
        List<UserCVResponse> cvs = userCVService.getMyCVs(getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.success(cvs, "Lấy danh sách CV thành công"));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<UserCV>> uploadCV(@RequestParam("file") MultipartFile file,
                                                       @RequestParam("cvName") String cvName) {
        UserCV cv = userCVService.uploadCV(getCurrentUserEmail(), file, cvName);
        return ResponseEntity.ok(ApiResponse.success(cv, "Tải CV lên thành công!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCV(@PathVariable Long id) {
        userCVService.deleteCV(id, getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa CV thành công!"));
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<ApiResponse<Void>> setDefaultCV(@PathVariable Long id) {
        userCVService.setDefaultCV(id, getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật CV mặc định thành công!"));
    }
}