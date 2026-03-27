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

        // 1. Validate: Mật khẩu và Xác nhận mật khẩu phải giống nhau
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Lỗi: Mật khẩu xác nhận không khớp!");
        }

        // 2. Validate: Độ dài mật khẩu
        if (request.getPassword().length() < 6) {
            throw new RuntimeException("Lỗi: Mật khẩu phải có ít nhất 6 ký tự!");
        }

        // 3. Validate: Định dạng Email
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new RuntimeException("Lỗi: Định dạng email không hợp lệ!");
        }

        // 4. Validate: Email đã tồn tại chưa
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Lỗi: Email này đã được đăng ký trong hệ thống!");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
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