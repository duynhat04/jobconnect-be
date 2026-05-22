package com.jobconnect.service;

import com.jobconnect.config.JwtUtils;
import com.jobconnect.entity.User;
import com.jobconnect.entity.AuthProvider;
import com.jobconnect.dto.JwtResponse;
import com.jobconnect.dto.RegisterRequest;
import com.jobconnect.dto.UserProfileDto;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Collections;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmailService emailService;

    @Value("${google.client.id}")
    private String googleClientId;

    public User registerUser(RegisterRequest request) {

        // 1. Confirm password
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }
        // 2. Password length
        if (request.getPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu phải >= 6 ký tự!");
        }
        // 3. Email format
        if (!request.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new RuntimeException("Email không hợp lệ!");
        }
        // 4. Check email tồn tại
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }

        // 5. Tạo user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("CANDIDATE");
        user.setProvider(AuthProvider.LOCAL);

        String otpCode = generateOtp();
        user.setOtpCode(otpCode);
        user.setOtpExpiredAt(
                LocalDateTime.now().plusMinutes(5));
        user.setEmailVerified(false);
        User savedUser = userRepository.save(user);

        // Gửi email chào mừng
        try {

            emailService.sendEmail(
                    savedUser.getEmail(),
                    "Mã xác thực JobConnect",
                    """
                            Xin chào %s,
                            Mã OTP xác thực tài khoản của bạn là:
                            %s
                            OTP sẽ hết hạn sau 5 phút.
                            JobConnect Team
                            """.formatted(
                            savedUser.getFullName(),
                            otpCode));

        } catch (Exception e) {
            // Không throw để tránh register fail chỉ vì mail lỗi
            System.out.println(
                    "Không thể gửi email welcome: "
                            + e.getMessage());
        }
        return savedUser;
    }

    public JwtResponse loginUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lỗi: Email không tồn tại!"));

        if (user.getProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new RuntimeException(
                    "Tài khoản này được đăng nhập bằng Google. Vui lòng dùng nút Đăng nhập với Google!");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Tài khoản chưa xác thực email!");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Lỗi: Sai mật khẩu!");
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        return new JwtResponse(
                token,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole());
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public void verifyOtp(String email, String otp) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));
        if (user.isEmailVerified()) {
            throw new RuntimeException("Email đã xác thực!");
        }
        if (user.getOtpCode() == null) {
            throw new RuntimeException("OTP không tồn tại!");
        }
        if (!user.getOtpCode().equals(otp)) {
            throw new RuntimeException("OTP không chính xác!");
        }
        if (user.getOtpExpiredAt() == null ||
                user.getOtpExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP đã hết hạn!");
        }
        user.setEmailVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);
        userRepository.save(user);
    }

    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));
        if (user.isEmailVerified()) {
            throw new RuntimeException(
                    "Email đã xác thực!");
        }
        String newOtp = generateOtp();
        user.setOtpCode(newOtp);
        user.setOtpExpiredAt(
                LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        emailService.sendEmail(
                user.getEmail(),
                "OTP mới xác thực JobConnect",
                """
                        Xin chào %s,
                        OTP mới của bạn là:
                        %s
                        OTP sẽ hết hạn sau 5 phút.
                        JobConnect Team
                        """.formatted(
                        user.getFullName(),
                        newOtp));
    }

    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Kiểm tra mật khẩu cũ có khớp không
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        // Mã hóa mật khẩu mới và lưu lại
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 1. Lấy thông tin Profile
    public UserProfileDto getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        return new UserProfileDto(
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getBio(),
                user.getSkills(), // Thêm
                user.getCvUrl() // Thêm
        );
    }

    // 2. Cập nhật Profile
    public UserProfileDto updateUserProfile(String email, UserProfileDto profileDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        user.setFullName(profileDto.getFullName());
        user.setPhone(profileDto.getPhone());
        user.setAddress(profileDto.getAddress());
        user.setBio(profileDto.getBio());
        user.setSkills(profileDto.getSkills());
        user.setCvUrl(profileDto.getCvUrl());

        userRepository.save(user);

        return new UserProfileDto(
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getBio(),
                user.getSkills(),
                user.getCvUrl());
    }

    public JwtResponse loginWithGoogle(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                throw new RuntimeException("Google token không hợp lệ!");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String fullName = (String) payload.get("name");
            String avatarUrl = (String) payload.get("picture");
            Boolean emailVerified = payload.getEmailVerified();

            if (email == null || email.isBlank()) {
                throw new RuntimeException("Không lấy được email từ Google!");
            }

            if (emailVerified == null || !emailVerified) {
                throw new RuntimeException("Email Google chưa được xác thực!");
            }

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setFullName(fullName != null ? fullName : email);
                user.setAvatarUrl(avatarUrl);
                user.setRole("CANDIDATE");
                user.setProvider(AuthProvider.GOOGLE);
                user.setEmailVerified(true);
                user.setPassword(null);

                user = userRepository.save(user);
            } else {
                if (user.getProvider() == AuthProvider.LOCAL) {
                    user.setProvider(AuthProvider.GOOGLE);
                }

                user.setEmailVerified(true);

                if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
                    user.setAvatarUrl(avatarUrl);
                }

                userRepository.save(user);
            }

            String token = jwtUtils.generateToken(user.getEmail(), user.getRole());
            String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

            return new JwtResponse(
                    token,
                    refreshToken,
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole());

        } catch (Exception e) {
            throw new RuntimeException("Đăng nhập Google thất bại: " + e.getMessage());
        }
    }
}