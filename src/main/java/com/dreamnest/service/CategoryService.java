package com.dreamnest.service;

import com.dreamnest.dto.request.CategoryRequest;
import com.dreamnest.dto.response.CategoryResponse;

import java.util.List;

/**
 * Manages product categories.
 */
public interface CategoryService {

    List<CategoryResponse> getAllActiveCategories();

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
