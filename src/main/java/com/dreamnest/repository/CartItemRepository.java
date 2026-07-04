package com.dreamnest.repository;

import com.dreamnest.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndProductIdAndSize(Long cartId, Long productId, com.dreamnest.enums.ProductSize size);
    void deleteByCartId(Long cartId);
}
