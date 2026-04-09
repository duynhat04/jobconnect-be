package com.jobconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String fullName;

    // Thêm các trường cho trang Profile
    private String phone;
    private String address;
    private String avatarUrl; // Chứa link ảnh

    @Column(columnDefinition = "TEXT")
    private String bio; // Giới thiệu bản thân

    // Mặc định ai đăng ký cũng là Ứng viên
    @Column(nullable = false)
    private String role = "CANDIDATE";

    // Trạng thái tài khoản (true = hoạt động, false = bị khóa)
    // Mặc định tạo mới là true (hoạt động)
    private boolean isActive = true; 
}