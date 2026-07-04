package com.dreamnest.service;

import com.dreamnest.dto.request.NewsRequest;
import com.dreamnest.dto.response.NewsResponse;
import com.dreamnest.dto.response.PageResponse;

import java.util.List;

/**
 * Manages news/announcement content.
 */
public interface NewsService {

    PageResponse<NewsResponse> getPublishedNews(String category, int page, int size);

    NewsResponse getNewsById(Long id);

    List<NewsResponse> getAllNews();

    NewsResponse createNews(NewsRequest request);

    NewsResponse updateNews(Long id, NewsRequest request);

    void deleteNews(Long id);
}
