package com.dreamnest.service.impl;

import com.dreamnest.dto.request.NewsRequest;
import com.dreamnest.dto.response.NewsResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.entity.News;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.NewsMapper;
import com.dreamnest.repository.NewsRepository;
import com.dreamnest.service.NewsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link NewsService}.
 */
@Service
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;

    public NewsServiceImpl(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Override
    public PageResponse<NewsResponse> getPublishedNews(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (category != null && !category.isBlank()) {
            return PageResponse.from(newsRepository.findByCategory(category, pageable).map(NewsMapper::toResponse));
        }
        return PageResponse.from(newsRepository.findByActiveTrueOrderByPublishDateDesc(pageable).map(NewsMapper::toResponse));
    }

    @Override
    public NewsResponse getNewsById(Long id) {
        return NewsMapper.toResponse(findNews(id));
    }

    @Override
    public List<NewsResponse> getAllNews() {
        return newsRepository.findAll(Sort.by("publishDate").descending()).stream()
                .map(NewsMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NewsResponse createNews(NewsRequest request) {
        News news = new News();
        NewsMapper.updateEntity(news, request);
        if (request.getActive() == null) {
            news.setActive(true);
        }
        news = newsRepository.save(news);
        return NewsMapper.toResponse(news);
    }

    @Override
    @Transactional
    public NewsResponse updateNews(Long id, NewsRequest request) {
        News news = findNews(id);
        NewsMapper.updateEntity(news, request);
        news = newsRepository.save(news);
        return NewsMapper.toResponse(news);
    }

    @Override
    @Transactional
    public void deleteNews(Long id) {
        News news = findNews(id);
        newsRepository.delete(news);
    }

    private News findNews(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", id));
    }
}
