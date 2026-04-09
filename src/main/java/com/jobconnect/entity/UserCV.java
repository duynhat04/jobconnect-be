package com.jobconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_cvs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCV {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String cvName; // Tên gợi nhớ (VD: CV Backend 2024)
    private String fileUrl; // Link file đã upload (Cloudinary/S3 hoặc thư mục local)

    private String cloudinaryPublicId;
    private boolean isDefault = false; // Đánh dấu CV chính

    private LocalDateTime uploadedAt = LocalDateTime.now();
}