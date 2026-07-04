package com.dreamnest.service;

import com.dreamnest.dto.request.ReviewRequest;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.ReviewResponse;

/**
 * Manages product reviews.
 */
public interface ReviewService {

    PageResponse<ReviewResponse> getReviewsForProduct(Long productId, int page, int size);

    ReviewResponse createReview(Long userId, Long productId, ReviewRequest request);
}
