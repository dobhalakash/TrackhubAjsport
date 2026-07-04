package com.dreamnest.service.impl;

import com.dreamnest.dto.request.ReviewRequest;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.ReviewResponse;
import com.dreamnest.entity.Product;
import com.dreamnest.entity.Review;
import com.dreamnest.entity.User;
import com.dreamnest.exception.DuplicateResourceException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.ReviewMapper;
import com.dreamnest.repository.ProductRepository;
import com.dreamnest.repository.ReviewRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementation of {@link ReviewService}.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PageResponse<ReviewResponse> getReviewsForProduct(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(reviewRepository.findByProductId(productId, pageable).map(ReviewMapper::toResponse));
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, Long productId, ReviewRequest request) {
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("You have already reviewed this product");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Review review = new Review(user, product, request.getRating(), request.getComment());
        review = reviewRepository.save(review);

        Double average = reviewRepository.findAverageRatingByProductId(productId);
        product.setAverageRating(average != null ?
                BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        productRepository.save(product);

        return ReviewMapper.toResponse(review);
    }
}
