package com.jobconnect.service;

import com.jobconnect.config.JwtUtils;
import com.jobconnect.dto.JwtResponse;
import com.jobconnect.dto.RegisterRequest;
import com.jobconnect.dto.UserProfileDto;
import com.jobconnect.dto.password.ChangePasswordRequest;
import com.jobconnect.dto.password.ForgotPasswordRequest;
import com.jobconnect.dto.password.ResetPasswordRequest;
import com.jobconnect.entity.AuthProvider;
import com.jobconnect.entity.User;
import com.jobconnect.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;

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

    @Autowired
    private CloudinaryStorageService cloudinaryStorageService;

    @Value("${google.client.id}")
    private String googleClientId;

    // =========================
    // REGISTER
    // =========================
    public User registerUser(RegisterRequest request) {
        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            throw new RuntimeException("Vui lòng nhập đầy đủ mật khẩu!");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        validateStrongPassword(request.getPassword());

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Vui lòng nhập email!");
        }

        String email = request.getEmail().trim().toLowerCase();

        if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new RuntimeException("Email không hợp lệ!");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("CANDIDATE");
        user.setProvider(AuthProvider.LOCAL);

        String otpCode = generateOtp();
        user.setOtpCode(otpCode);
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(5));
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

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
                    """.formatted(savedUser.getFullName(), otpCode)
            );
        } catch (Exception e) {
            System.out.println("Không thể gửi email OTP: " + e.getMessage());
        }

        return savedUser;
    }

    // =========================
    // LOGIN LOCAL
    // =========================
    public JwtResponse loginUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lỗi: Email không tồn tại!"));

        if (user.getProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new RuntimeException(
                    "Tài khoản này được đăng nhập bằng Google. Vui lòng dùng nút Đăng nhập với Google!"
            );
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
                user.getRole(),
                user.getAvatarUrl()
        );
    }

    // =========================
    // OTP
    // =========================
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
            throw new RuntimeException("Email đã xác thực!");
        }

        String newOtp = generateOtp();

        user.setOtpCode(newOtp);
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(5));

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
                """.formatted(user.getFullName(), newOtp)
        );
    }

    // =========================
    // PROFILE
    // =========================
    public UserProfileDto getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateUserProfile(String email, UserProfileDto profileDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        user.setFullName(profileDto.getFullName());
        user.setPhone(profileDto.getPhone());
        user.setAddress(profileDto.getAddress());
        user.setBio(profileDto.getBio());
        user.setSkills(profileDto.getSkills());
        user.setCvUrl(profileDto.getCvUrl());

        user.setDesiredPosition(profileDto.getDesiredPosition());
        user.setDesiredCategory(profileDto.getDesiredCategory());
        user.setExperienceYears(profileDto.getExperienceYears());
        user.setExpectedSalary(profileDto.getExpectedSalary());
        user.setWorkType(profileDto.getWorkType());
        user.setEducationLevel(profileDto.getEducationLevel());
        user.setEnglishLevel(profileDto.getEnglishLevel());
        user.setCertificates(profileDto.getCertificates());
        user.setProjects(profileDto.getProjects());
        user.setAvailableFrom(profileDto.getAvailableFrom());

        User savedUser = userRepository.save(user);

        return mapToProfileDto(savedUser);
    }

    // =========================
    // UPLOAD AVATAR
    // =========================
    @Transactional
    public UserProfileDto uploadAvatar(String email, MultipartFile avatarFile) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        String avatarUrl = cloudinaryStorageService.uploadAvatar(avatarFile, user.getId());

        user.setAvatarUrl(avatarUrl);

        User savedUser = userRepository.save(user);

        return mapToProfileDto(savedUser);
    }

    // =========================
    // LOGIN GOOGLE
    // =========================
    public JwtResponse loginWithGoogle(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
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

                user = userRepository.save(user);
            }

            String token = jwtUtils.generateToken(user.getEmail(), user.getRole());
            String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

            return new JwtResponse(
                    token,
                    refreshToken,
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.getAvatarUrl()
            );

        } catch (Exception e) {
            throw new RuntimeException("Đăng nhập Google thất bại: " + e.getMessage());
        }
    }

    // =========================
    // CHANGE PASSWORD
    // =========================
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        if (user.getProvider() == AuthProvider.GOOGLE || user.getPassword() == null) {
            throw new RuntimeException("Tài khoản này đăng nhập bằng Google, không thể đổi mật khẩu trong hệ thống!");
        }

        if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu cũ!");
        }

        if (request.getNewPassword() == null || request.getConfirmPassword() == null) {
            throw new RuntimeException("Vui lòng nhập đầy đủ mật khẩu mới!");
        }

        validateStrongPassword(request.getNewPassword());

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu cũ!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }

    // =========================
    // FORGOT PASSWORD
    // =========================
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Vui lòng nhập email!");
        }

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        if (user.getProvider() == AuthProvider.GOOGLE || user.getPassword() == null) {
            throw new RuntimeException(
                    "Tài khoản này đăng nhập bằng Google, vui lòng sử dụng Google để quản lý mật khẩu!"
            );
        }

        String otp = generateOtp();

        user.setResetPasswordOtp(otp);
        user.setResetPasswordOtpExpiredAt(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        String subject = "Mã OTP đặt lại mật khẩu JobConnect";
        String body = """
                Xin chào %s,

                Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản JobConnect.

                Mã OTP đặt lại mật khẩu của bạn là:
                %s

                Mã OTP sẽ hết hạn sau 5 phút.
                Nếu bạn không yêu cầu thao tác này, vui lòng bỏ qua email này.

                JobConnect Team
                """.formatted(user.getFullName(), otp);

        emailService.sendEmail(user.getEmail(), subject, body);
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Vui lòng nhập email!");
        }

        if (request.getOtp() == null || request.getOtp().isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã OTP!");
        }

        if (request.getNewPassword() == null || request.getConfirmPassword() == null) {
            throw new RuntimeException("Vui lòng nhập đầy đủ mật khẩu mới!");
        }

        validateStrongPassword(request.getNewPassword());

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        if (user.getProvider() == AuthProvider.GOOGLE || user.getPassword() == null) {
            throw new RuntimeException(
                    "Tài khoản này đăng nhập bằng Google, vui lòng sử dụng Google để quản lý mật khẩu!"
            );
        }

        if (user.getResetPasswordOtp() == null || user.getResetPasswordOtpExpiredAt() == null) {
            throw new RuntimeException("Bạn chưa yêu cầu đặt lại mật khẩu!");
        }

        if (!user.getResetPasswordOtp().equals(request.getOtp().trim())) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }

        if (user.getResetPasswordOtpExpiredAt().isBefore(LocalDateTime.now())) {
            user.setResetPasswordOtp(null);
            user.setResetPasswordOtpExpiredAt(null);
            userRepository.save(user);

            throw new RuntimeException("Mã OTP đã hết hạn, vui lòng yêu cầu mã mới!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu cũ!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordOtp(null);
        user.setResetPasswordOtpExpiredAt(null);
        user.setEmailVerified(true);

        userRepository.save(user);
    }

    // =========================
    // HELPER
    // =========================
    private UserProfileDto mapToProfileDto(User user) {
        return new UserProfileDto(
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getBio(),
                user.getSkills(),
                user.getCvUrl(),
                user.getAvatarUrl(),

                user.getDesiredPosition(),
                user.getDesiredCategory(),
                user.getExperienceYears(),
                user.getExpectedSalary(),
                user.getWorkType(),
                user.getEducationLevel(),
                user.getEnglishLevel(),
                user.getCertificates(),
                user.getProjects(),
                user.getAvailableFrom()
        );
    }

    private String generateOtp() {
        Random random = new Random();

        int otp = 100000 + random.nextInt(900000);

        return String.valueOf(otp);
    }

    private void validateStrongPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu!");
        }

        if (password.length() < 8) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự!");
        }

        if (password.contains(" ")) {
            throw new RuntimeException("Mật khẩu không được chứa khoảng trắng!");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ thường!");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ hoa!");
        }

        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ số!");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 ký tự đặc biệt!");
        }
    }
}