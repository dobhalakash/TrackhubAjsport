package com.dreamnest.controller;

import com.dreamnest.dto.request.ReviewRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.ReviewResponse;
import com.dreamnest.service.ReviewService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for browsing and submitting product reviews.
 */
@RestController
@RequestMapping("/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(reviewService.getReviewsForProduct(productId, page, size));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@PathVariable Long productId,
                                                                      @Valid @RequestBody ReviewRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ReviewResponse response = reviewService.createReview(userId, productId, request);
        return new ResponseEntity<>(ApiResponse.success("Review submitted successfully", response), HttpStatus.CREATED);
    }
}
