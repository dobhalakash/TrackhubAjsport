package com.dreamnest.controller;

import com.dreamnest.dto.request.NewsRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.NewsResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for browsing news content (public) and managing it (admin).
 */
@RestController
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/news")
    public ApiResponse<PageResponse<NewsResponse>> getPublishedNews(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(newsService.getPublishedNews(category, page, size));
    }

    @GetMapping("/news/{id}")
    public ApiResponse<NewsResponse> getNews(@PathVariable Long id) {
        return ApiResponse.success(newsService.getNewsById(id));
    }

    // ---- Admin management ----

    @GetMapping("/admin/news")
    public ApiResponse<List<NewsResponse>> getAllNews() {
        return ApiResponse.success(newsService.getAllNews());
    }

    @PostMapping("/admin/news")
    public ResponseEntity<ApiResponse<NewsResponse>> createNews(@Valid @RequestBody NewsRequest request) {
        NewsResponse response = newsService.createNews(request);
        return new ResponseEntity<>(ApiResponse.success("News item created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/admin/news/{id}")
    public ApiResponse<NewsResponse> updateNews(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
        return ApiResponse.success("News item updated successfully", newsService.updateNews(id, request));
    }

    @DeleteMapping("/admin/news/{id}")
    public ApiResponse<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ApiResponse.success("News item deleted successfully", null);
    }
}
