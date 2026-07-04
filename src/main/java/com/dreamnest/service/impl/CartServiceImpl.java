package com.dreamnest.service.impl;

import com.dreamnest.dto.request.AddCartItemRequest;
import com.dreamnest.dto.request.MergeCartRequest;
import com.dreamnest.dto.request.UpdateCartItemRequest;
import com.dreamnest.dto.response.CartItemResponse;
import com.dreamnest.dto.response.CartResponse;
import com.dreamnest.entity.Cart;
import com.dreamnest.entity.CartItem;
import com.dreamnest.entity.Product;
import com.dreamnest.entity.User;
import com.dreamnest.enums.RoleName;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.CartMapper;
import com.dreamnest.repository.CartItemRepository;
import com.dreamnest.repository.CartRepository;
import com.dreamnest.repository.ProductRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CartService}.
 * The cart is persisted in the database for authenticated users; the Angular frontend
 * is responsible for maintaining a localStorage cart for guests and calling
 * {@link #mergeCart} on login to combine the two.
 */
@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository,
                            CartItemRepository cartItemRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        requireCustomer(userId);
        Cart cart = getOrCreateCart(userId);

        if (request.getProductId() == null) {
            throw new BadRequestException("A product must be specified");
        }

        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        CartItem existing = cartItemRepository
                .findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), request.getSize())
                .orElse(null);

        if (existing != null && !existing.isSavedForLater()) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem(cart, product, request.getSize(), quantity, product.getFinalPrice());
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        cart = cartRepository.save(cart);
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findItem(cart, itemId);

        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getSavedForLater() != null) {
            item.setSavedForLater(request.getSavedForLater());
        }
        cartItemRepository.save(item);

        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    @Override
    @Transactional
    public CartResponse mergeCart(Long userId, MergeCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        // Merging runs automatically after every login/registration - silently
        // skip rather than error out, since business/admin accounts simply
        // aren't supposed to have a shopping cart, not because something went wrong.
        if (!isCustomer(userId)) {
            return CartMapper.toResponse(cart);
        }

        if (request.getItems() != null) {
            for (AddCartItemRequest itemRequest : request.getItems()) {
                try {
                    addItem(userId, itemRequest);
                } catch (ResourceNotFoundException ex) {
                    // Skip items referencing products that no longer exist
                }
            }
        }

        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return CartMapper.toResponse(cart);
    }

    @Override
    public List<CartItemResponse> getSavedForLater(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cart.getItems().stream()
                .filter(CartItem::isSavedForLater)
                .map(CartMapper::toItemResponse)
                .collect(Collectors.toList());
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    return cartRepository.save(new Cart(user));
                });
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));
    }

    /** Business and admin accounts browse the catalog but never buy - they manage their own shop, not shop on it. */
    private void requireCustomer(Long userId) {
        if (!isCustomer(userId)) {
            throw new BadRequestException("Business and admin accounts cannot purchase products. Please use a customer account to shop.");
        }
    }

    private boolean isCustomer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return user.getRole() != null && user.getRole().getName() == RoleName.CUSTOMER;
    }
}
