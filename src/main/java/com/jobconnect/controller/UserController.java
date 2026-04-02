package com.jobconnect.controller;

import com.jobconnect.dto.LoginRequest;
import com.jobconnect.entity.User;
import com.jobconnect.service.UserService;
import com.jobconnect.dto.RegisterRequest;
import com.jobconnect.config.JwtUtils;
import com.jobconnect.dto.JwtResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // 1. Tạo user
            User user = userService.registerUser(request);

            // 2. Tạo token luôn
            String token = jwtUtils.generateToken(user.getEmail());

            // 3. Trả về giống login
            return ResponseEntity.ok(new JwtResponse(token, user));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // API ĐĂNG NHẬP
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 1. Kiểm tra email & password xem có chuẩn không
            User user = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());

            // 2. Nếu chuẩn, in cho 1 cái Token
            String token = jwtUtils.generateToken(user.getEmail());

            // 3. Trả về cả Token và thông tin User
            return ResponseEntity.ok(new JwtResponse(token, user));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}