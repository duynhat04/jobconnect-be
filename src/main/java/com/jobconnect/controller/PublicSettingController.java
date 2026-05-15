package com.jobconnect.controller;

import com.jobconnect.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class PublicSettingController {

    @Autowired
    private SystemSettingService settingService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getPublicSettings() {
        return ResponseEntity.ok(settingService.getAllSettings());
    }
}