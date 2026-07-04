package com.dreamnest.mapper;

import com.dreamnest.dto.response.ReviewResponse;
import com.dreamnest.entity.Review;

/**
 * Maps {@link Review} entities to response DTOs.
 */
public class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUser() != null ? review.getUser().getId() : null);
        response.setUserName(review.getUser() != null ?
                (review.getUser().getFirstName() + " " +
                        (review.getUser().getLastName() != null ? review.getUser().getLastName() : "")).trim() : null);
        response.setProductId(review.getProduct() != null ? review.getProduct().getId() : null);
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
