package com.jobconnect.service;

import com.jobconnect.entity.User;
import com.jobconnect.dto.RegisterRequest;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Trông sạch sẽ và chuyên nghiệp hơn hẳn đúng không!
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
        user.setPassword(passwordEncoder.encode(request.getPassword())); // ✅ CHUẨN
        user.setRole("CANDIDATE");

        return userRepository.save(user);
    }

    // --- HÀM ĐĂNG NHẬP MỚI THÊM VÀO ---
    public User loginUser(String email, String password) {
        // Bước 1: Tìm User bằng Email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lỗi: Email không tồn tại!"));

        // Bước 2: Kiểm tra mật khẩu (DÙNG BCRYPT ĐỂ SO SÁNH (Mật khẩu gõ vào vs Mật khẩu đã mã hóa trong DB) )
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Lỗi: Sai mật khẩu!");
        }
        // Bước 3: Đăng nhập thành công, trả về thông tin User
        return user;
    }
}