package com.jobconnect.service;

import com.cloudinary.Cloudinary;
import com.jobconnect.entity.User;
import com.jobconnect.entity.UserCV;
import com.jobconnect.repository.UserCVRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class UserCVService {

    @Autowired
    private UserCVRepository userCVRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Cloudinary cloudinary; // Tái sử dụng Bean Cloudinary

    public List<UserCV> getMyCVs(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return userCVRepository.findByUserId(user.getId());
    }

    public UserCV uploadCV(String email, MultipartFile file, String cvName) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // 1. Tương tự như bên JobApplicationService: Đẩy file lên Cloudinary
        Map<String, Object> uploadOptions = new java.util.HashMap<>();
        uploadOptions.put("resource_type", "auto"); // Tự động nhận diện file PDF/Docx

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
        String fileUrl = uploadResult.get("secure_url").toString();

        // 2. Tạo bản ghi UserCV lưu vào Database
        UserCV newCV = new UserCV();
        newCV.setUser(user);
        newCV.setCvName(cvName);
        newCV.setFileUrl(fileUrl);

        // Nếu đây là cái CV đầu tiên user up lên, mặc định gán nó là CV chính
        if (userCVRepository.findByUserId(user.getId()).isEmpty()) {
            newCV.setDefault(true);
        }

        return userCVRepository.save(newCV);
    }

    public void deleteCV(Long id, String email) {
        // Bảo mật nhẹ: Lấy CV ra xem có đúng là của thằng đang đăng nhập không rồi mới xóa
        UserCV cv = userCVRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV"));

        if (!cv.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Lỗi bảo mật: Bạn không có quyền xóa CV này!");
        }

        userCVRepository.deleteById(id);

        // Ghi chú: Nếu ông muốn xịn thì phải viết thêm hàm gọi API Cloudinary để xóa file trên Cloud đi cho đỡ tốn dung lượng, nhưng tạm thời cứ xóa DB trước đã.
    }
}