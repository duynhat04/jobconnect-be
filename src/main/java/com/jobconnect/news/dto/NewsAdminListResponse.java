package com.jobconnect.news.dto;

import com.jobconnect.news.NewsStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NewsAdminListResponse {

    private Long id;

    private String title;

    private String slug;

    private String summary;

    private String thumbnailUrl;

    private NewsStatus status;

    private LocalDateTime publishedAt;

    private Long viewCount;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}