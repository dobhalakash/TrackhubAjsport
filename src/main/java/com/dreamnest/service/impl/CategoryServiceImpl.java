package com.dreamnest.service.impl;

import com.dreamnest.dto.request.CategoryRequest;
import com.dreamnest.dto.response.CategoryResponse;
import com.dreamnest.entity.Category;
import com.dreamnest.exception.DuplicateResourceException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.CategoryMapper;
import com.dreamnest.repository.CategoryRepository;
import com.dreamnest.service.CategoryService;
import com.dreamnest.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CategoryService}.
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = findCategory(id);
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }
        Category category = new Category();
        String slug = generateUniqueSlug(request.getName(), null);
        CategoryMapper.updateEntity(category, request, slug);
        if (request.getActive() == null) {
            category.setActive(true);
        }
        category = categoryRepository.save(category);
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);

        if (!category.getName().equalsIgnoreCase(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }

        String slug = category.getSlug();
        if (!category.getName().equalsIgnoreCase(request.getName())) {
            slug = generateUniqueSlug(request.getName(), id);
        }

        CategoryMapper.updateEntity(category, request, slug);
        category = categoryRepository.save(category);
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private String generateUniqueSlug(String name, Long excludeId) {
        String baseSlug = SlugUtil.toSlug(name);
        String slug = baseSlug;
        int counter = 1;
        while (categoryRepository.findBySlug(slug)
                .filter(c -> excludeId == null || !c.getId().equals(excludeId))
                .isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}
