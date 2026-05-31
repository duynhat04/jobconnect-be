package com.jobconnect.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryStorageService {

    private final Cloudinary cloudinary;

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final long MAX_CV_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private static final Set<String> ALLOWED_CV_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    public String uploadAvatar(MultipartFile file, Long userId) {
        validateFile(
                file,
                MAX_AVATAR_SIZE,
                ALLOWED_AVATAR_TYPES,
                "Ảnh đại diện chỉ hỗ trợ JPG, PNG hoặc WEBP!",
                "Ảnh đại diện không được vượt quá 2MB!");

        return upload(
                file,
                "jobconnect/avatars",
                "avatar_user_" + userId + "_" + UUID.randomUUID());
    }

    public String uploadNewsThumbnail(MultipartFile file, Long adminId) {
        validateFile(
                file,
                MAX_AVATAR_SIZE,
                ALLOWED_AVATAR_TYPES,
                "Ảnh bài viết chỉ hỗ trợ JPG, PNG hoặc WEBP!",
                "Ảnh bài viết không được vượt quá 2MB!");

        return upload(
                file,
                "jobconnect/news",
                "news_thumbnail_" + adminId + "_" + UUID.randomUUID());
    }

    public String uploadCv(MultipartFile file, Long userId) {
        validateFile(
                file,
                MAX_CV_SIZE,
                ALLOWED_CV_TYPES,
                "CV chỉ hỗ trợ PDF, DOC hoặc DOCX!",
                "Kích thước file CV quá lớn. Vui lòng upload file dưới 5MB!");

        return upload(
                file,
                "jobconnect/cvs",
                "cv_user_" + userId + "_" + UUID.randomUUID());
    }

    private String upload(MultipartFile file, String folder, String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", publicId,
                            "resource_type", "auto",
                            "overwrite", true));

            Object secureUrl = result.get("secure_url");

            if (secureUrl == null) {
                throw new RuntimeException("Không lấy được URL file từ Cloudinary!");
            }

            return secureUrl.toString();
        } catch (Exception e) {
            throw new RuntimeException("Upload file lên Cloudinary thất bại: " + e.getMessage());
        }
    }

    private void validateFile(
            MultipartFile file,
            long maxSize,
            Set<String> allowedTypes,
            String invalidTypeMessage,
            String maxSizeMessage) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file để upload!");
        }

        if (file.getSize() > maxSize) {
            throw new RuntimeException(maxSizeMessage);
        }

        String contentType = file.getContentType();

        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new RuntimeException(invalidTypeMessage);
        }
    }
}