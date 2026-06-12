package com.jobconnect.dto;

import com.jobconnect.entity.EmploymentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private Long id;

    private String title;

    private String description;

    private String location;

    private Long salary;

    private String requirements;

    private String category;

    private EmploymentType employmentType;

    // Ngày đăng bài
    private LocalDateTime createdAt;

    // Ngày hết hạn ứng tuyển
    private LocalDate expiredAt;

    // PENDING / APPROVED / REJECTED / CLOSED
    private String status;

    private String rejectionReason;

    private Long companyId;

    private String companyName;

    private String companyLogo;

    private String companyAddress;

    // =========================
    // APPLICATION STATISTICS
    // =========================

    // Tổng số ứng viên đã apply job này
    private long applicationCount;

    // Số ứng viên chờ xử lý
    private long pendingApplicationCount;

    // Số ứng viên đã xem
    private long reviewedApplicationCount;

    // Số ứng viên được chọn
    private long acceptedApplicationCount;

    // Số ứng viên bị từ chối
    private long rejectedApplicationCount;

    // =========================
    // DISPLAY STATUS
    // =========================

    // Job đã hết hạn theo expiredAt chưa
    private boolean expired;

    // Job đã bị NTD đóng tuyển chưa
    private boolean closed;

    // Còn bao nhiêu ngày hết hạn
    private long daysRemaining;
}