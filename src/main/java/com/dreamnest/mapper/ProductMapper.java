package com.dreamnest.mapper;

import com.dreamnest.dto.response.ProductImageResponse;
import com.dreamnest.dto.response.ProductResponse;
import com.dreamnest.dto.response.ProductSummaryResponse;
import com.dreamnest.dto.response.ProductVariantResponse;
import com.dreamnest.entity.Product;
import com.dreamnest.entity.ProductImage;
import com.dreamnest.entity.ProductVariant;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps {@link Product} entities to response DTOs.
 */
public class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setDiscountPercentage(product.getDiscountPercentage());
        response.setFinalPrice(product.getFinalPrice());
        response.setStock(product.getStock());
        response.setSku(product.getSku());
        response.setBrand(product.getBrand());
        response.setTrending(product.isTrending());
        response.setActive(product.isActive());
        response.setCodEnabled(product.isCodEnabled());
        response.setCodAdvanceAmount(product.getCodAdvanceAmount());
        response.setAverageRating(product.getAverageRating());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());

        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());
            response.setCategoryName(product.getCategory().getName());
            response.setCategorySizeGuide(product.getCategory().getSizeGuide());
        }
        if (product.getBusinessUser() != null) {
            response.setBusinessUserId(product.getBusinessUser().getId());
            response.setBusinessName(product.getBusinessUser().getFirstName() + " " +
                    (product.getBusinessUser().getLastName() != null ? product.getBusinessUser().getLastName() : ""));
        }

        if (product.getImages() != null) {
            response.setImages(product.getImages().stream()
                    .map(ProductMapper::toImageResponse)
                    .collect(Collectors.toList()));
        }
        if (product.getVariants() != null) {
            response.setVariants(product.getVariants().stream()
                    .map(ProductMapper::toVariantResponse)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    public static ProductSummaryResponse toSummaryResponse(Product product) {
        if (product == null) {
            return null;
        }
        String primaryImage = null;
        List<ProductImage> images = product.getImages();
        if (images != null && !images.isEmpty()) {
            primaryImage = images.stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .orElse(images.stream()
                            .min(Comparator.comparing(img -> img.getDisplayOrder() == null ? 0 : img.getDisplayOrder()))
                            .orElse(images.get(0)))
                    .getImageUrl();
        }

        ProductSummaryResponse response = new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDiscountPercentage(),
                product.getFinalPrice(),
                product.getBrand(),
                product.isTrending(),
                product.getStock(),
                product.getAverageRating(),
                primaryImage,
                product.getCategory() != null ? product.getCategory().getName() : null
        );
        response.setCodEnabled(product.isCodEnabled());
        response.setCodAdvanceAmount(product.getCodAdvanceAmount());
        return response;
    }

    public static ProductImageResponse toImageResponse(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getImageUrl(), image.isPrimary(), image.getDisplayOrder());
    }

    public static ProductVariantResponse toVariantResponse(ProductVariant variant) {
        return new ProductVariantResponse(variant.getId(), variant.getSize(), variant.getFitType(),
                variant.getStock(), variant.getSkuSuffix());
    }
}
