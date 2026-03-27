package com.jobconnect.entity;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(nullable = false)
    private String title;

    private String location;
    private Long salary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private String status = "PENDING"; // Trạng thái mặc định khi vừa tạo là PENDING

    @Column(columnDefinition = "TEXT")
    private String rejectionReason; // Lưu lý do nếu Admin từ chối

    // Liên kết Nhiều-1 với Company (Nhiều Job thuộc về 1 Company)
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}