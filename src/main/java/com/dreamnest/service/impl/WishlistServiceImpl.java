package com.dreamnest.service.impl;

import com.dreamnest.dto.response.ProductSummaryResponse;
import com.dreamnest.entity.Product;
import com.dreamnest.entity.User;
import com.dreamnest.entity.Wishlist;
import com.dreamnest.exception.DuplicateResourceException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.ProductMapper;
import com.dreamnest.repository.ProductRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.repository.WishlistRepository;
import com.dreamnest.service.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link WishlistService}.
 */
@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistServiceImpl(WishlistRepository wishlistRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ProductSummaryResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(Wishlist::getProduct)
                .map(ProductMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addToWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("Product is already in your wishlist");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        wishlistRepository.save(new Wishlist(user, product));
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
