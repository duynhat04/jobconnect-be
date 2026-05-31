package com.jobconnect.controller;

import com.jobconnect.entity.User;
import com.jobconnect.service.UserService;
import com.jobconnect.dto.RegisterRequest;
import com.jobconnect.dto.UserProfileDto;
import com.jobconnect.dto.password.ChangePasswordRequest;
import com.jobconnect.dto.password.ForgotPasswordRequest;
import com.jobconnect.dto.password.ResetPasswordRequest;
import com.jobconnect.dto.JwtResponse;
import com.jobconnect.dto.VerifyOtpRequest;
import com.jobconnect.dto.ResendOtpRequest;
import com.jobconnect.dto.LoginRequest;
import com.jobconnect.dto.GoogleLoginRequest;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.config.JwtUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // 1. Tạo user
            User user = userService.registerUser(request);

            // 2. Tạo token luôn
            String token = jwtUtils.generateToken(user.getEmail(), user.getRole());

            // 3. Trả về giống login
            String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());
            return ResponseEntity.ok(new JwtResponse(token, refreshToken, user.getId(), user.getEmail(),
                    user.getFullName(), user.getRole(), user.getAvatarUrl()));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API ĐĂNG NHẬP
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Vì UserService.loginUser của sếp đã trả về JwtResponse rồi
            // nên ở đây gọi trực tiếp luôn cho gọn sạch
            JwtResponse response = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            JwtResponse response = userService.loginWithGoogle(request.getCredential());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody VerifyOtpRequest request) {
        try {
            userService.verifyOtp(
                    request.getEmail(),
                    request.getOtp());
            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Xác thực email thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(
            @RequestBody ResendOtpRequest request) {
        try {
            userService.resendOtp(
                    request.getEmail());
            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Đã gửi OTP mới!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            String email = getCurrentUserEmail();
            userService.changePassword(email, request);

            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.forgotPassword(request);

            return ResponseEntity.ok(Map.of("message", "Mã OTP đặt lại mật khẩu đã được gửi đến email của bạn!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request);

            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // API CẤP LẠI TOKEN MỚI
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken != null && jwtUtils.validateToken(refreshToken)) {
            String email = jwtUtils.getEmailFromToken(refreshToken);

            // Lấy role từ DB để tạo Access Token mới có đầy đủ quyền hạn
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                String newToken = jwtUtils.generateToken(email, user.getRole());
                return ResponseEntity.ok(Map.of("token", newToken));
            }
        }
        return ResponseEntity.status(401).body(Map.of("message", "Refresh Token không hợp lệ!"));
    }

    // 1. API LẤY THÔNG TIN PROFILE
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            UserProfileDto profile = userService.getUserProfile(email);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tải hồ sơ: " + e.getMessage()));
        }
    }

    // 2. API CẬP NHẬT THÔNG TIN PROFILE
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserProfileDto profileDto) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            UserProfileDto updatedProfile = userService.updateUserProfile(email, profileDto);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi cập nhật hồ sơ: " + e.getMessage()));
        }
    }

    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof com.jobconnect.entity.User) {
            return ((com.jobconnect.entity.User) principal).getEmail();
        }

        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        return principal.toString();
    }

    @PutMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            Authentication authentication) {
        try {
            UserProfileDto updatedProfile = userService.uploadAvatar(
                    authentication.getName(),
                    avatar);

            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi cập nhật ảnh đại diện: " + e.getMessage()));
        }
    }
}