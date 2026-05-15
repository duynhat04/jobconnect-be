package com.jobconnect.controller;

import com.jobconnect.entity.JobPackage;
import com.jobconnect.service.JobPackageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/packages")
@CrossOrigin(origins = "*") 
public class JobPackageController {

    @Autowired
    private JobPackageService packageService;

    // API lấy gói hiển thị cho người dùng (Chỉ lấy gói Active)
    @GetMapping("/active")
    public ResponseEntity<List<JobPackage>> getActivePackages() {
        return ResponseEntity.ok(packageService.getActivePackages());
    }
    
    @GetMapping
    public ResponseEntity<List<JobPackage>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    @PostMapping
    public ResponseEntity<JobPackage> createPackage(@Valid @RequestBody JobPackage jobPackage) {
        return ResponseEntity.ok(packageService.createPackage(jobPackage));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobPackage> updatePackage(
            @PathVariable Long id, 
            @Valid @RequestBody JobPackage jobPackage) {
        return ResponseEntity.ok(packageService.updatePackage(id, jobPackage));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<String> togglePackage(@PathVariable Long id) {
        packageService.togglePackageStatus(id);
        return ResponseEntity.ok("Đã thay đổi trạng thái gói dịch vụ thành công!");
    }
}