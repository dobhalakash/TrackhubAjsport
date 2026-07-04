package com.dreamnest.service.impl;

import com.dreamnest.dto.request.ProductRequest;
import com.dreamnest.dto.request.ProductVariantRequest;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.ProductResponse;
import com.dreamnest.dto.response.ProductSummaryResponse;
import com.dreamnest.entity.Category;
import com.dreamnest.entity.Product;
import com.dreamnest.entity.ProductImage;
import com.dreamnest.entity.ProductVariant;
import com.dreamnest.entity.StockAlert;
import com.dreamnest.entity.User;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.DuplicateResourceException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.exception.UnauthorizedException;
import com.dreamnest.mapper.ProductMapper;
import com.dreamnest.repository.CategoryRepository;
import com.dreamnest.repository.ProductRepository;
import com.dreamnest.repository.StockAlertRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.service.EmailService;
import com.dreamnest.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ProductService}.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StockAlertRepository stockAlertRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public ProductServiceImpl(ProductRepository productRepository,
                               CategoryRepository categoryRepository,
                               UserRepository userRepository,
                               StockAlertRepository stockAlertRepository,
                               EmailService emailService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.stockAlertRepository = stockAlertRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> searchProducts(Long categoryId, String brand, BigDecimal minPrice,
                                                                 BigDecimal maxPrice, String keyword,
                                                                 int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy);
        sort = "asc".equalsIgnoreCase(sortDir) ? sort.ascending() : sort.descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products = productRepository.searchAndFilter(categoryId, brand, minPrice, maxPrice, keyword, pageable);
        Page<ProductSummaryResponse> mapped = products.map(ProductMapper::toSummaryResponse);
        return PageResponse.from(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getRelatedProducts(Long productId, int limit) {
        Product product = findProduct(productId);
        if (product.getCategory() == null) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, Math.min(limit, 20));
        return productRepository.findRelated(product.getCategory().getId(), productId, pageable).stream()
                .map(ProductMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getSearchSuggestions(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        Pageable top8 = PageRequest.of(0, 8);
        return productRepository.findSuggestions(query.trim(), top8).stream()
                .map(ProductMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getTrendingProducts() {
        return productRepository.findByTrendingTrueAndActiveTrue().stream()
                .map(ProductMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProduct(id);
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByBusiness(Long businessUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByBusinessUserId(businessUserId, pageable);
        return PageResponse.from(products.map(ProductMapper::toResponse));
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long businessUserId) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        User businessUser = userRepository.findById(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", businessUserId));

        String sku = request.getSku();
        if (sku == null || sku.isBlank()) {
            sku = generateSku();
        } else if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("A product with this SKU already exists");
        }

        Product product = new Product(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO,
                request.getStock(),
                sku,
                request.getBrand(),
                request.getTrending() != null && request.getTrending(),
                category,
                businessUser
        );
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getCodEnabled() != null) {
            product.setCodEnabled(request.getCodEnabled());
        }
        if (request.getCodAdvanceAmount() != null) {
            product.setCodAdvanceAmount(request.getCodAdvanceAmount());
        }

        applyImagesAndVariants(product, request);

        product = productRepository.save(product);
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long businessUserId, boolean isAdmin) {
        Product product = findProduct(id);
        ensureOwnership(product, businessUserId, isAdmin);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        if (request.getSku() != null && !request.getSku().isBlank() && !request.getSku().equals(product.getSku())
                && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("A product with this SKU already exists");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO);
        Integer previousStock = product.getStock();
        product.setStock(request.getStock());
        if (request.getSku() != null && !request.getSku().isBlank()) {
            product.setSku(request.getSku());
        }
        product.setBrand(request.getBrand());
        if (request.getTrending() != null) {
            product.setTrending(request.getTrending());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getCodEnabled() != null) {
            product.setCodEnabled(request.getCodEnabled());
        }
        if (request.getCodAdvanceAmount() != null) {
            product.setCodAdvanceAmount(request.getCodAdvanceAmount());
        }
        product.setCategory(category);

        // Replace images and variants if provided
        if (request.getImageUrls() != null) {
            product.getImages().clear();
            applyImages(product, request.getImageUrls());
        }
        if (request.getVariants() != null) {
            product.getVariants().clear();
            applyVariants(product, request.getVariants());
        }

        product = productRepository.save(product);

        if ((previousStock == null || previousStock <= 0) && product.getStock() != null && product.getStock() > 0) {
            notifyStockAlertSubscribers(product);
        }

        return ProductMapper.toResponse(product);
    }

    private void notifyStockAlertSubscribers(Product product) {
        java.util.List<StockAlert> pending = stockAlertRepository.findByProductIdAndNotifiedFalse(product.getId());
        for (StockAlert alert : pending) {
            String productUrl = frontendUrl + "/products/" + product.getId();
            // Best-effort: a failed email shouldn't block the stock update or other subscribers.
            try {
                emailService.sendBackInStockEmail(alert.getEmail(), product.getName(), productUrl);
            } catch (Exception ignored) {
            }
            alert.setNotified(true);
        }
        stockAlertRepository.saveAll(pending);
    }

    @Override
    @Transactional
    public void subscribeToStockAlert(Long productId, String email) {
        Product product = findProduct(productId);
        if (product.getStock() != null && product.getStock() > 0) {
            throw new BadRequestException("This product is already in stock");
        }
        if (stockAlertRepository.existsByProductIdAndEmailAndNotifiedFalse(productId, email)) {
            return; // already subscribed, nothing to do
        }
        stockAlertRepository.save(new StockAlert(product, email));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Long businessUserId, boolean isAdmin) {
        Product product = findProduct(id);
        ensureOwnership(product, businessUserId, isAdmin);
        product.setActive(false);
        productRepository.save(product);
    }

    private void applyImagesAndVariants(Product product, ProductRequest request) {
        if (request.getImageUrls() != null) {
            applyImages(product, request.getImageUrls());
        }
        if (request.getVariants() != null) {
            applyVariants(product, request.getVariants());
        }
    }

    private void applyImages(Product product, List<String> imageUrls) {
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(new ProductImage(imageUrls.get(i), i == 0, i, product));
        }
        product.getImages().addAll(images);
    }

    private void applyVariants(Product product, List<ProductVariantRequest> variantRequests) {
        List<ProductVariant> variants = new ArrayList<>();
        for (ProductVariantRequest vr : variantRequests) {
            variants.add(new ProductVariant(product, vr.getSize(), vr.getFitType(),
                    vr.getStock() != null ? vr.getStock() : 0, vr.getSkuSuffix()));
        }
        product.getVariants().addAll(variants);
    }

    private void ensureOwnership(Product product, Long businessUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (product.getBusinessUser() == null || !product.getBusinessUser().getId().equals(businessUserId)) {
            throw new UnauthorizedException("You do not have permission to modify this product");
        }
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private String generateSku() {
        String sku;
        do {
            sku = "TH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (productRepository.existsBySku(sku));
        return sku;
    }
}
