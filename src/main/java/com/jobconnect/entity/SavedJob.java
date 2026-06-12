package com.jobconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "saved_jobs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "job_id"})
        },
        indexes = {
                @Index(name = "idx_saved_jobs_user_id", columnList = "user_id"),
                @Index(name = "idx_saved_jobs_job_id", columnList = "job_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ứng viên lưu việc làm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Việc làm được lưu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private LocalDateTime savedAt;
}