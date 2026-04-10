package com.jobconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private String fullName;

    private String phone;
    private String avatarUrl;

    @Column(nullable = false)
    private String role = "CANDIDATE"; // "CANDIDATE", "EMPLOYER", "ADMIN"

    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status = UserStatus.ACTIVE;

    private String address;

    @Column(columnDefinition = "TEXT")
    private String bio; // Giới thiệu bản thân (Mục Tiêu Nghề Nghiệp)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt; // Ngày tạo tài khoản

    @UpdateTimestamp
    private LocalDateTime updatedAt; // Lần cập nhật cuối cùng

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Company company;
}