package com.jobconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_applications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"job_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Công việc ứng tuyển
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // Ứng viên ứng tuyển
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Link CV Cloudinary hoặc CV đã lưu
    @Column(name = "cv_url", nullable = false, length = 1000)
    private String cvUrl;

    // Thư ứng tuyển / lời nhắn
    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    // Snapshot thông tin ứng viên tại thời điểm apply
    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "candidate_email")
    private String candidateEmail;

    @Column(name = "candidate_phone")
    private String candidatePhone;

    @Column(name = "candidate_address")
    private String candidateAddress;

    @Column(name = "candidate_skills", columnDefinition = "TEXT")
    private String candidateSkills;

    @Column(name = "candidate_desired_position")
    private String candidateDesiredPosition;

    @Column(name = "candidate_desired_category")
    private String candidateDesiredCategory;

    @Column(name = "candidate_experience_years")
    private Integer candidateExperienceYears;

    @Column(name = "candidate_expected_salary")
    private Long candidateExpectedSalary;

    @Column(name = "candidate_work_type")
    private String candidateWorkType;

    @Column(name = "candidate_education_level")
    private String candidateEducationLevel;

    @Column(name = "candidate_english_level")
    private String candidateEnglishLevel;

    @Column(name = "candidate_certificates", columnDefinition = "TEXT")
    private String candidateCertificates;

    @Column(name = "candidate_projects", columnDefinition = "TEXT")
    private String candidateProjects;

    @Column(name = "candidate_available_from")
    private String candidateAvailableFrom;

    @Column(name = "candidate_portfolio_url", length = 1000)
    private String candidatePortfolioUrl;

    @Column(name = "candidate_linkedin_url", length = 1000)
    private String candidateLinkedinUrl;

    // PENDING / REVIEWED / ACCEPTED / REJECTED
    @Column(nullable = false)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;
}