package com.jobconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên công việc
    @Column(nullable = false)
    private String title;

    // Địa điểm làm việc
    private String location;

    // Lương
    private Long salary;

    // Ngành nghề / danh mục
    @Column(name = "category")
    private String category;

    // Mô tả công việc
    @Column(columnDefinition = "TEXT")
    private String description;

    // Yêu cầu công việc
    @Column(columnDefinition = "TEXT")
    private String requirements;

    // Hình thức tuyển dụng
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    // Ngày đăng bài
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Ngày hết hạn bài đăng
    @Column(name = "expired_at", nullable = false)
    private LocalDate expiredAt;

    // Trạng thái bài đăng
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}