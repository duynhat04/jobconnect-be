package com.jobconnect.news;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "news_articles",
        indexes = {
                @Index(name = "idx_news_slug", columnList = "slug", unique = true),
                @Index(name = "idx_news_status", columnList = "status"),
                @Index(name = "idx_news_published_at", columnList = "published_at"),
                @Index(name = "idx_news_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, unique = true, length = 260)
    private String slug;

    @Column(length = 500)
    private String summary;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NewsStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (viewCount == null) {
            viewCount = 0L;
        }

        if (status == null) {
            status = NewsStatus.DRAFT;
        }

        if (status == NewsStatus.PUBLISHED && publishedAt == null) {
            publishedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (status == NewsStatus.PUBLISHED && publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }
}