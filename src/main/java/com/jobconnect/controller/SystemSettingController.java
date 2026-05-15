package com.jobconnect.controller;

import com.jobconnect.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class SystemSettingController {

    @Autowired
    private SystemSettingService settingService;

    // 1. API GET: Lấy cấu hình đổ vào form
    // Đã được bảo vệ tự động bởi SecurityConfig (chỉ ADMIN mới gọi được /api/admin/**)
    @GetMapping
    public ResponseEntity<Map<String, String>> getSettings() {
        return ResponseEntity.ok(settingService.getAllSettings());
    }

    // 2. API PUT: Nhận formData từ Frontend để lưu
    // Đã được bảo vệ tự động bởi SecurityConfig
    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> settings) {
        try {
            settingService.updateSettings(settings);
            return ResponseEntity.ok("Đã lưu cấu hình hệ thống thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lưu cấu hình: " + e.getMessage());
        }
    }
}