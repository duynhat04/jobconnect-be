package com.jobconnect.news;

import com.jobconnect.news.dto.NewsAdminListResponse;
import com.jobconnect.news.dto.NewsDetailResponse;
import com.jobconnect.news.dto.NewsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
public class NewsAdminController {

    private final NewsService newsService;

    @GetMapping
    public Page<NewsAdminListResponse> searchAdminNews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) NewsStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return newsService.searchAdmin(keyword, status, page, size);
    }

    @GetMapping("/{id}")
    public NewsDetailResponse getAdminDetail(@PathVariable Long id) {
        return newsService.getAdminDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NewsDetailResponse createNews(
            @Valid @RequestBody NewsRequest request,
            Authentication authentication) {
        String adminEmail = authentication != null
                ? authentication.getName()
                : "ADMIN";

        return newsService.createNews(request, adminEmail);
    }

    @PutMapping("/{id}")
    public NewsDetailResponse updateNews(
            @PathVariable Long id,
            @Valid @RequestBody NewsRequest request,
            Authentication authentication) {
        String adminEmail = authentication != null
                ? authentication.getName()
                : "ADMIN";

        return newsService.updateNews(id, request, adminEmail);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
    }

    @PostMapping("/upload-thumbnail")
    public ResponseEntity<?> uploadThumbnail(
            @RequestParam("image") MultipartFile image,
            Authentication authentication) {
        try {
            String imageUrl = newsService.uploadThumbnail(
                    image,
                    authentication.getName());

            return ResponseEntity.ok(Map.of(
                    "url", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Lỗi upload ảnh bài viết: " + e.getMessage()));
        }
    }
}