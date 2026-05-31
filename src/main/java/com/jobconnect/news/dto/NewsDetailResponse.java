package com.jobconnect.news.dto;

import com.jobconnect.news.NewsStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NewsDetailResponse {

    private Long id;

    private String title;

    private String slug;

    private String summary;

    private String thumbnailUrl;

    private String content;

    private NewsStatus status;

    private LocalDateTime publishedAt;

    private Long viewCount;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}