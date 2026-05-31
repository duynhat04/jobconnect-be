package com.jobconnect.news;

import com.jobconnect.news.dto.NewsAdminListResponse;
import com.jobconnect.news.dto.NewsDetailResponse;
import com.jobconnect.news.dto.NewsListResponse;
import com.jobconnect.news.dto.NewsRequest;
import com.jobconnect.entity.User;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.service.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final CloudinaryStorageService cloudinaryStorageService;
    private final UserRepository userRepository;

    private static final int MAX_PUBLIC_PAGE_SIZE = 30;
    private static final int MAX_ADMIN_PAGE_SIZE = 50;

    private final NewsRepository newsRepository;

    // ================= ADMIN =================

    @Transactional
    public NewsDetailResponse createNews(NewsRequest request, String adminEmail) {
        NewsStatus status = request.getStatus() != null
                ? request.getStatus()
                : NewsStatus.DRAFT;

        String title = cleanRequiredText(request.getTitle(), "Tiêu đề không được để trống!");
        String content = cleanRequiredText(request.getContent(), "Nội dung không được để trống!");

        NewsArticle article = NewsArticle.builder()
                .title(title)
                .slug(generateUniqueSlug(title, null))
                .summary(cleanNullableText(request.getSummary()))
                .thumbnailUrl(cleanNullableText(request.getThumbnailUrl()))
                .content(content)
                .status(status)
                .publishedAt(resolvePublishedAt(status, request.getPublishedAt()))
                .viewCount(0L)
                .createdBy(adminEmail)
                .updatedBy(adminEmail)
                .build();

        return toDetailResponse(newsRepository.save(article));
    }

    @Transactional
    public NewsDetailResponse updateNews(Long id, NewsRequest request, String adminEmail) {
        NewsArticle article = newsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy bài viết tin tức!"));

        NewsStatus status = request.getStatus() != null
                ? request.getStatus()
                : article.getStatus();

        String title = cleanRequiredText(request.getTitle(), "Tiêu đề không được để trống!");
        String content = cleanRequiredText(request.getContent(), "Nội dung không được để trống!");

        article.setTitle(title);
        article.setSlug(generateUniqueSlug(title, id));
        article.setSummary(cleanNullableText(request.getSummary()));
        article.setThumbnailUrl(cleanNullableText(request.getThumbnailUrl()));
        article.setContent(content);
        article.setStatus(status);
        article.setPublishedAt(resolvePublishedAt(status, request.getPublishedAt()));
        article.setUpdatedBy(adminEmail);

        return toDetailResponse(newsRepository.save(article));
    }

    @Transactional
    public void deleteNews(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy bài viết tin tức!");
        }

        newsRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public NewsDetailResponse getAdminDetail(Long id) {
        NewsArticle article = newsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy bài viết tin tức!"));

        return toDetailResponse(article);
    }

    @Transactional(readOnly = true)
    public Page<NewsAdminListResponse> searchAdmin(
            String keyword,
            NewsStatus status,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size, MAX_ADMIN_PAGE_SIZE));

        String cleanKeyword = normalizeKeyword(keyword);

        if (cleanKeyword != null && status != null) {
            return newsRepository.searchAdminListByStatus(cleanKeyword, status, pageable);
        }

        if (cleanKeyword != null) {
            return newsRepository.searchAdminList(cleanKeyword, pageable);
        }

        if (status != null) {
            return newsRepository.findAdminListByStatus(status, pageable);
        }

        return newsRepository.findAdminList(pageable);
    }

    // ================= PUBLIC =================

    @Transactional(readOnly = true)
    public Page<NewsListResponse> searchPublic(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size, MAX_PUBLIC_PAGE_SIZE));

        String cleanKeyword = normalizeKeyword(keyword);
        LocalDateTime now = LocalDateTime.now();

        if (cleanKeyword == null) {
            return newsRepository.findPublicPublishedList(now, pageable);
        }

        return newsRepository.searchPublicPublishedList(cleanKeyword, now, pageable);
    }

    @Transactional
    public NewsDetailResponse getPublicDetail(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Slug bài viết không hợp lệ!");
        }

        NewsArticle article = newsRepository.findPublicDetail(slug.trim(), LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bài viết không tồn tại hoặc chưa được xuất bản!"));

        newsRepository.increaseViewCount(article.getId());

        article.setViewCount(article.getViewCount() + 1);

        return toDetailResponse(article);
    }

    // ================= MAPPER =================

    private NewsDetailResponse toDetailResponse(NewsArticle article) {
        return NewsDetailResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .summary(article.getSummary())
                .thumbnailUrl(article.getThumbnailUrl())
                .content(article.getContent())
                .status(article.getStatus())
                .publishedAt(article.getPublishedAt())
                .viewCount(article.getViewCount())
                .createdBy(article.getCreatedBy())
                .updatedBy(article.getUpdatedBy())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    // ================= HELPER =================

    private int normalizePageSize(int size, int maxSize) {
        return Math.min(Math.max(size, 1), maxSize);
    }

    private String cleanRequiredText(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private String cleanNullableText(String value) {
        if (value == null) {
            return null;
        }

        String clean = value.trim();

        return clean.isEmpty() ? null : clean;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String clean = keyword.trim().replaceAll("\\s+", " ");

        if (clean.isEmpty()) {
            return null;
        }

        return clean;
    }

    private LocalDateTime resolvePublishedAt(NewsStatus status, LocalDateTime publishedAt) {
        if (status == NewsStatus.PUBLISHED) {
            return publishedAt != null ? publishedAt : LocalDateTime.now();
        }

        return publishedAt;
    }

    private String generateUniqueSlug(String title, Long currentId) {
        String baseSlug = toSlug(title);

        if (baseSlug.isBlank()) {
            baseSlug = "bai-viet";
        }

        String slug = baseSlug;
        int counter = 1;

        while (isSlugExists(slug, currentId)) {
            counter++;
            slug = baseSlug + "-" + counter;
        }

        return slug;
    }

    private boolean isSlugExists(String slug, Long currentId) {
        if (currentId == null) {
            return newsRepository.existsBySlug(slug);
        }

        return newsRepository.existsBySlugAndIdNot(slug, currentId);
    }

    private String toSlug(String input) {
        String lowerCase = input.toLowerCase(Locale.ROOT).trim();

        String normalized = Normalizer.normalize(lowerCase, Normalizer.Form.NFD);

        String withoutAccent = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized)
                .replaceAll("");

        withoutAccent = withoutAccent.replace("đ", "d");

        return withoutAccent
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    @Transactional
    public String uploadThumbnail(MultipartFile image, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản admin!"));

        return cloudinaryStorageService.uploadNewsThumbnail(image, admin.getId());
    }
}