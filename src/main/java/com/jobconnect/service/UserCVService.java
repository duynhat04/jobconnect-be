package com.jobconnect.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jobconnect.dto.UserCVResponse;
import com.jobconnect.entity.User;
import com.jobconnect.entity.UserCV;
import com.jobconnect.repository.UserCVRepository;
import com.jobconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor // Chuẩn Deploy: Khuyên dùng thay cho @Autowired trên từng field
public class UserCVService {

    private final UserCVRepository userCVRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;

    public List<UserCVResponse> getMyCVs(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        return userCVRepository.findByUserId(user.getId())
                .stream()
                .map(cv -> new UserCVResponse(
                        cv.getId(),
                        cv.getCvName(),
                        cv.getFileUrl(),
                        cv.isDefault(),
                        cv.getUploadedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class) // Rollback DB nếu bất kỳ lỗi nào xảy ra
    public UserCV uploadCV(String email, MultipartFile file, String cvName) {
        // 1. Validate file chặt chẽ hơn
        if (file == null || file.isEmpty()) throw new RuntimeException("File không được để trống!");
        if (file.getSize() > 5 * 1024 * 1024) throw new RuntimeException("File tối đa 5MB!");
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) throw new RuntimeException("Chỉ chấp nhận file PDF!");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        try {
            // 2. FIX LỖI CLOUDINARY: Chuyển MultipartFile sang byte[] thay vì dùng InputStream
            byte[] fileBytes = file.getBytes();

            // 3. Cấu hình Upload Cloudinary
            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", "raw", // Bắt buộc là "raw" đối với file PDF/Docx
                    "folder", "jobconnect/cv",
                    "public_id", "cv_" + user.getId() + "_" + System.currentTimeMillis()+ ".pdf"
            );

            // Thực hiện đẩy file lên
            Map result = cloudinary.uploader().upload(fileBytes, options);
            
            String fileUrl = result.get("secure_url").toString();
            String publicId = result.get("public_id").toString();

            // 4. Lưu Database
            UserCV newCV = new UserCV();
            newCV.setUser(user);
            newCV.setCvName(cvName);
            newCV.setFileUrl(fileUrl);
            newCV.setCloudinaryPublicId(publicId);

            // Nếu user chưa có CV nào trong DB, mặc định CV đầu tiên là Default
            boolean isFirstCV = userCVRepository.findByUserId(user.getId()).isEmpty();
            newCV.setDefault(isFirstCV);

            return userCVRepository.save(newCV);

        } catch (IOException e) {
            log.error("❌ Cloudinary upload error for user {}: ", email, e);
            throw new RuntimeException("Lỗi máy chủ trong quá trình tải file lên hệ thống!");
        }
    }

    @Transactional
    public void deleteCV(Long id, String email) {
        UserCV cv = userCVRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV yêu cầu!"));

        if (!cv.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền xóa CV này!");
        }

        // Xóa file vật lý trên Cloudinary trước
        if (cv.getCloudinaryPublicId() != null && !cv.getCloudinaryPublicId().trim().isEmpty()) {
            try {
                cloudinary.uploader().destroy(cv.getCloudinaryPublicId(), ObjectUtils.asMap("resource_type", "raw"));
            } catch (IOException e) {
                log.warn("⚠️ Không thể xóa file trên Cloudinary (public_id: {}), nhưng vẫn tiến hành xóa trong DB.", cv.getCloudinaryPublicId(), e);
                // Cố tình không throw Exception ở đây.
                // Nếu Cloudinary lỗi mạng, ta vẫn muốn xóa dữ liệu rác trong Database.
            }
        }

        // Xóa dữ liệu trong DB
        userCVRepository.delete(cv);
    }

    @Transactional
    public void setDefaultCV(Long cvId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        UserCV targetCV = userCVRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV không tồn tại!"));

        if (!targetCV.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không sở hữu CV này!");
        }

        // Tối ưu thuật toán set Default: Chỉ 1 vòng lặp duy nhất
        List<UserCV> userCvs = userCVRepository.findByUserId(user.getId());
        userCvs.forEach(cv -> {
            // Nếu CV đang duyệt trùng ID với CV mục tiêu -> set true, còn lại false
            cv.setDefault(cv.getId().equals(targetCV.getId()));
        });

        // Save 1 lần toàn bộ List thay vì save từng cái
        userCVRepository.saveAll(userCvs);
    }
}