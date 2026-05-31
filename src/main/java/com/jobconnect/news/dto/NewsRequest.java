package com.jobconnect.news.dto;

import com.jobconnect.news.NewsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NewsRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 220, message = "Tiêu đề không được vượt quá 220 ký tự")
    private String title;

    @Size(max = 500, message = "Mô tả ngắn không được vượt quá 500 ký tự")
    private String summary;

    @Size(max = 1000, message = "URL ảnh không được vượt quá 1000 ký tự")
    private String thumbnailUrl;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content;

    private NewsStatus status;

    private LocalDateTime publishedAt;
}