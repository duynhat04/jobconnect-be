package com.jobconnect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "job_packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên gói không được để trống")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Giá tiền không được để trống")
    @Min(value = 0, message = "Giá tiền không được nhỏ hơn 0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price; // Dùng BigDecimal để tính tiền cực chuẩn

    @NotNull(message = "Số lượt đăng không được để trống")
    @Min(value = 1, message = "Số lượt đăng phải từ 1 trở lên")
    @Column(name = "post_limit", nullable = false)
    private Integer postLimit;

    @NotNull(message = "Thời hạn không được để trống")
    @Min(value = 1, message = "Thời hạn phải từ 1 ngày trở lên")
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "is_popular")
    private Boolean isPopular = false;

    @Column(name = "is_active")
    private Boolean isActive = true;
}