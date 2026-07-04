package com.dreamnest.mapper;

import com.dreamnest.dto.request.CategoryRequest;
import com.dreamnest.dto.response.CategoryResponse;
import com.dreamnest.entity.Category;

/**
 * Maps {@link Category} entities to/from DTOs.
 */
public class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        CategoryResponse response = new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getCategoryType(),
                category.isActive()
        );
        response.setSizeGuide(category.getSizeGuide());
        return response;
    }

    public static void updateEntity(Category category, CategoryRequest request, String slug) {
        category.setName(request.getName());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setSizeGuide(request.getSizeGuide());
        category.setCategoryType(request.getCategoryType());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
    }
}
