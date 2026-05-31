package com.jobconnect.news.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NewsListResponse {

    private Long id;

    private String title;

    private String slug;

    private String summary;

    private String thumbnailUrl;

    private LocalDateTime publishedAt;

    private Long viewCount;
}