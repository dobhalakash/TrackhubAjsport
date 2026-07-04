package com.dreamnest.mapper;

import com.dreamnest.dto.request.NewsRequest;
import com.dreamnest.dto.response.NewsResponse;
import com.dreamnest.entity.News;

/**
 * Maps {@link News} entities to/from DTOs.
 */
public class NewsMapper {

    private NewsMapper() {
    }

    public static NewsResponse toResponse(News news) {
        if (news == null) {
            return null;
        }
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setDescription(news.getDescription());
        response.setThumbnailUrl(news.getThumbnailUrl());
        response.setPublishDate(news.getPublishDate());
        response.setCategory(news.getCategory());
        response.setActive(news.isActive());
        return response;
    }

    public static void updateEntity(News news, NewsRequest request) {
        news.setTitle(request.getTitle());
        news.setDescription(request.getDescription());
        news.setThumbnailUrl(request.getThumbnailUrl());
        news.setPublishDate(request.getPublishDate());
        news.setCategory(request.getCategory());
        if (request.getActive() != null) {
            news.setActive(request.getActive());
        }
    }
}
