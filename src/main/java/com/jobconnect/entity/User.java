package com.jobconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobconnect.entity.AuthProvider;
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
    @Column
    private String password;

    @Column(nullable = false)
    private String fullName;

    private String phone;
    private String avatarUrl;

    @Column(nullable = false)
    private String role = "CANDIDATE"; // "CANDIDATE", "EMPLOYER", "ADMIN"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(length = 10)
    private String otpCode;

    private LocalDateTime otpExpiredAt;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status = UserStatus.ACTIVE;

    private String address;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Company company;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(name = "cv_url")
    private String cvUrl;

    @Column(name = "desired_position")
    private String desiredPosition;

    @Column(name = "desired_category")
    private String desiredCategory;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "expected_salary")
    private Long expectedSalary;

    @Column(name = "work_type")
    private String workType;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "english_level")
    private String englishLevel;

    @Column(columnDefinition = "TEXT")
    private String certificates;

    @Column(columnDefinition = "TEXT")
    private String projects;

    @Column(name = "available_from")
    private String availableFrom;
}