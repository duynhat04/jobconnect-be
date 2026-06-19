package com.jobconnect.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final CloudinaryStorageService cloudinaryStorageService;

    @Value("${google.client.id}")
    private String googleClientId;

    // =========================
    // REGISTER
    // =========================

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu đăng ký không hợp lệ!");
        }

        String fullName = normalize(request.getFullName());
        String email = normalizeEmail(request.getEmail());

        if (fullName.isBlank()) {
            throw new RuntimeException("Vui lòng nhập họ tên!");
        }

        if (email.isBlank()) {
            throw new RuntimeException("Vui lòng nhập email!");
        }

        if (!isValidEmail(email)) {
            throw new RuntimeException("Email không hợp lệ!");
        }

        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            throw new RuntimeException("Vui lòng nhập đầy đủ mật khẩu!");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        validateStrongPassword(request.getPassword());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }

        String otpCode = generateOtp();

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("CANDIDATE");
        user.setProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setOtpCode(otpCode);
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(5));

        User savedUser = userRepository.save(user);

        // Không được try/catch nuốt lỗi ở đây.
        // Nếu Gmail lỗi, API register phải báo lỗi để FE biết.
        emailService.sendRegisterOtpEmail(
                savedUser.getEmail(),
                savedUser.getFullName(),
                otpCode
        );

        return savedUser;
    }

    // =========================
    // LOGIN LOCAL
    // =========================

    public JwtResponse loginUser(String email, String password) {
        String safeEmail = normalizeEmail(email);

        if (safeEmail.isBlank()) {
            throw new RuntimeException("Vui lòng nhập email!");
        }

        if (password == null || password.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu!");
        }

        User user = userRepository.findByEmail(safeEmail)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        if (user.getProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new RuntimeException(
                    "Tài khoản này được đăng nhập bằng Google. Vui lòng dùng nút Đăng nhập với Google!"
            );
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Tài khoản chưa xác thực email. Vui lòng kiểm tra mã OTP trong Gmail!");
        }

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Sai mật khẩu!");
        }

        return buildJwtResponse(user);
    }

    // =========================
    // OTP
    // =========================

    @Transactional
    public User verifyOtp(String email, String otp) {
        String safeEmail = normalizeEmail(email);
        String safeOtp = normalize(otp);

        if (safeEmail.isBlank()) {
            throw new RuntimeException("Vui lòng nhập email!");
        }

        if (safeOtp.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã OTP!");
        }

        User user = userRepository.findByEmail(safeEmail)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        if (user.isEmailVerified()) {
            return user;
        }

        if (user.getOtpCode() == null || user.getOtpExpiredAt() == null) {
            throw new RuntimeException("OTP không tồn tại. Vui lòng yêu cầu gửi lại OTP!");
        }

        if (user.getOtpExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP đã hết hạn!");
        }

        if (!user.getOtpCode().equals(safeOtp)) {
            throw new RuntimeException("OTP không chính xác!");
        }

        user.setEmailVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);

        return userRepository.save(user);
    }

    @Transactional
    public void resendOtp(String email) {
        String safeEmail = normalizeEmail(email);

        if (safeEmail.isBlank()) {
            throw new RuntimeException("Vui lòng nhập email!");
        }

        User user = userRepository.findByEmail(safeEmail)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email đã xác thực!");
        }

        String newOtp = generateOtp();

        user.setOtpCode(newOtp);
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        emailService.sendResendOtpEmail(
                user.getEmail(),
                user.getFullName(),
                newOtp
        );
    }

    // =========================
    // PROFILE
    // =========================

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String email) {
        String safeEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(safeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + safeEmail));

        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateUserProfile(String email, UserProfileDto profileDto) {
        String safeEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(safeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + safeEmail));

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
        String safeEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(safeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        String avatarUrl = cloudinaryStorageService.uploadAvatar(avatarFile, user.getId());

        user.setAvatarUrl(avatarUrl);

        User savedUser = userRepository.save(user);

        return mapToProfileDto(savedUser);
    }

    // =========================
    // LOGIN GOOGLE
    // =========================

    @Transactional
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

            String email = normalizeEmail(payload.getEmail());
            String fullName = (String) payload.get("name");
            String avatarUrl = (String) payload.get("picture");
            Boolean emailVerified = payload.getEmailVerified();

            if (email.isBlank()) {
                throw new RuntimeException("Không lấy được email từ Google!");
            }

            if (emailVerified == null || !emailVerified) {
                throw new RuntimeException("Email Google chưa được xác thực!");
            }

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setFullName(normalize(fullName).isBlank() ? email : fullName.trim());
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

                if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())
                        && avatarUrl != null
                        && !avatarUrl.isBlank()) {
                    user.setAvatarUrl(avatarUrl);
                }

                user = userRepository.save(user);
            }

            return buildJwtResponse(user);

        } catch (Exception e) {
            throw new RuntimeException("Đăng nhập Google thất bại: " + e.getMessage());
        }
    }

    // =========================
    // CHANGE PASSWORD
    // =========================

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        String safeEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(safeEmail)
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

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Vui lòng nhập email!");
        }

        String email = normalizeEmail(request.getEmail());

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

        emailService.sendResetPasswordOtpEmail(
                user.getEmail(),
                user.getFullName(),
                otp
        );
    }

    @Transactional
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

        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        if (user.getProvider() == AuthProvider.GOOGLE || user.getPassword() == null) {
            throw new RuntimeException(
                    "Tài khoản này đăng nhập bằng Google, vui lòng sử dụng Google để quản lý mật khẩu!"
            );
        }

        if (user.getResetPasswordOtp() == null || user.getResetPasswordOtpExpiredAt() == null) {
            throw new RuntimeException("Bạn chưa yêu cầu đặt lại mật khẩu!");
        }

        if (user.getResetPasswordOtpExpiredAt().isBefore(LocalDateTime.now())) {
            user.setResetPasswordOtp(null);
            user.setResetPasswordOtpExpiredAt(null);
            userRepository.save(user);

            throw new RuntimeException("Mã OTP đã hết hạn, vui lòng yêu cầu mã mới!");
        }

        if (!user.getResetPasswordOtp().equals(request.getOtp().trim())) {
            throw new RuntimeException("Mã OTP không chính xác!");
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

    private JwtResponse buildJwtResponse(User user) {
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

    private UserProfileDto mapToProfileDto(User user) {
        return new UserProfileDto(
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
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
        int otp = 100000 + OTP_RANDOM.nextInt(900000);

        return String.valueOf(otp);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,}$");
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