package com.dreamnest.controller;

import com.dreamnest.dto.request.CategoryRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.CategoryResponse;
import com.dreamnest.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for browsing and managing product categories.
 * Read endpoints are public; write endpoints require BUSINESS_ADMIN or SUPER_ADMIN role
 * via the /business and /admin prefixed routes respectively.
 */
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getActiveCategories() {
        return ApiResponse.success(categoryService.getAllActiveCategories());
    }

    @GetMapping("/categories/{id}")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getCategoryById(id));
    }

    // ---- Admin management ----

    @GetMapping("/admin/categories")
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        return ApiResponse.success(categoryService.getAllCategories());
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(ApiResponse.success("Category created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/admin/categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("Category updated successfully", categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/admin/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success("Category deactivated successfully", null);
    }
}
