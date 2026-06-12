package com.jobconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên công ty
    @Column(nullable = false)
    private String name;

    // Mã số thuế
    @Column(name = "tax_code", unique = true, nullable = false)
    private String taxCode;

    // Địa chỉ công ty
    private String address;

    // Mô tả công ty
    @Column(columnDefinition = "TEXT")
    private String description;

    // Logo công ty
    private String logo;

    // Website công ty
    private String website;

    // Số điện thoại công ty
    private String phone;

    // Ảnh bìa công ty
    @Column(name = "cover_image", length = 500)
    private String coverImage;

    // Quy mô công ty
    @Column(name = "company_size", length = 100)
    private String companySize;

    // Lĩnh vực hoạt động
    @Column(length = 150)
    private String industry;

    // Chuyên môn chính
    @Column(length = 255)
    private String specialization;

    // Ngày tạo hồ sơ công ty
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Trạng thái duyệt: PENDING / APPROVED / REJECTED
    @Column(nullable = false)
    private String status = "PENDING";

    // Số lượt đăng tin còn lại
    @Column(name = "remaining_posts")
    private Integer remainingPosts = 5;

    // Tài khoản sở hữu công ty
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnore
    private User user;

}