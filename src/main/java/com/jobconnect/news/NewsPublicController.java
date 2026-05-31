package com.jobconnect.news;

import com.jobconnect.news.dto.NewsDetailResponse;
import com.jobconnect.news.dto.NewsListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsPublicController {

    private final NewsService newsService;

    @GetMapping
    public Page<NewsListResponse> getPublicNews(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        return newsService.searchPublic(keyword, page, size);
    }

    @GetMapping("/{slug}")
    public NewsDetailResponse getPublicDetail(@PathVariable String slug) {
        return newsService.getPublicDetail(slug);
    }
}