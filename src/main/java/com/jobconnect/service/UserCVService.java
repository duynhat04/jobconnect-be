package com.jobconnect.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
    private Cloudinary cloudinary;

    public List<UserCV> getMyCVs(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return userCVRepository.findByUserId(user.getId());
    }

    public UserCV uploadCV(String email, MultipartFile file, String cvName) throws Exception {
        // 1. Validation (Kiểm tra file)
        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống!");
        }
        
        // Check size: Max 5MB (5 * 1024 * 1024 bytes)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Kích thước file không được vượt quá 5MB!");
        }

        // Check type: Chỉ cho PDF hoặc Word
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && 
            !contentType.equals("application/msword") && 
            !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            throw new RuntimeException("Chỉ chấp nhận định dạng PDF hoặc Word (.doc, .docx)!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // 2. Upload lên Cloudinary
        Map<String, Object> uploadOptions = new java.util.HashMap<>();
        uploadOptions.put("resource_type", "auto"); 

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
        String fileUrl = uploadResult.get("secure_url").toString();
        // Lấy public_id để lưu vào DB
        String publicId = uploadResult.get("public_id").toString(); 

        // 3. Lưu vào Database
        UserCV newCV = new UserCV();
        newCV.setUser(user);
        newCV.setCvName(cvName);
        newCV.setFileUrl(fileUrl);
        newCV.setCloudinaryPublicId(publicId); // Lưu ID vào đây

        if (userCVRepository.findByUserId(user.getId()).isEmpty()) {
            newCV.setDefault(true);
        }

        return userCVRepository.save(newCV);
    }

    public void deleteCV(Long id, String email) throws Exception {
        UserCV cv = userCVRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV"));

        if (!cv.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Lỗi bảo mật: Bạn không có quyền xóa CV này!");
        }

        // Xóa file trên Cloudinary trước cho sạch server
        if (cv.getCloudinaryPublicId() != null) {
            cloudinary.uploader().destroy(cv.getCloudinaryPublicId(), ObjectUtils.emptyMap());
        }

        // Xóa trong DB
        userCVRepository.deleteById(id);
    }

    // Hàm Set CV mặc định
    public void setDefaultCV(Long cvId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        UserCV targetCV = userCVRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV"));

        if (!targetCV.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Lỗi bảo mật: Bạn không có quyền thao tác trên CV này!");
        }

        // Lấy tất cả CV của user này, set isDefault = false hết
        List<UserCV> userCvs = userCVRepository.findByUserId(user.getId());
        for (UserCV cv : userCvs) {
            cv.setDefault(false);
        }
        userCVRepository.saveAll(userCvs);

        // Bật isDefault = true cho cái CV được chọn
        targetCV.setDefault(true);
        userCVRepository.save(targetCV);
    }
}