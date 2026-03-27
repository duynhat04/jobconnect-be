package com.jobconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1 hồ sơ chỉ nộp cho 1 Job
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // 1 hồ sơ thuộc về 1 User (Ứng viên)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Link CV trả về từ Cloudinary
    @Column(nullable = false)
    private String cvUrl;

    // Lời nhắn gửi kèm (Cover Letter) - Có thể rỗng
    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    // Trạng thái hồ sơ: PENDING (Chờ duyệt), REVIEWED (Đã xem), ACCEPTED (Chấp nhận), REJECTED (Từ chối)
    @Column(nullable = false)
    private String status = "PENDING";

    private LocalDateTime appliedAt = LocalDateTime.now();
}