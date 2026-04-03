package com.jobconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String taxCode; // Mã số thuế không được trùng nhau

    private String address;

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả công ty (Dùng TEXT để lưu được nội dung dài)

    private String logo; // Lưu link ảnh logo (Cloudinary)

    private String website; // Link website công ty
    // Trạng thái: PENDING (Đang chờ duyệt), APPROVED (Đã duyệt), REJECTED (Từ chối)
    @Column(nullable = false)
    private String status = "PENDING";

    // Liên kết 1-1 với User (Người đại diện đăng ký công ty)
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore // Tránh lỗi lặp vô tận JSON khi test API
    private User user;
}