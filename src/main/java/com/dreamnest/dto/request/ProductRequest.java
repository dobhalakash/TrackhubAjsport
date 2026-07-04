package com.dreamnest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request payload for creating or updating a product.
 */
public class ProductRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "100.0", inclusive = true)
    private BigDecimal discountPercentage;

    @NotNull(message = "Stock is required")
    @Min(0)
    private Integer stock;

    private String sku;

    private String brand;

    private Boolean trending;

    private Boolean active;

    @NotNull(message = "Category is required")
    private Long categoryId;

    /** Whether this product can be ordered with Cash on Delivery. Only the owning business admin sets this. */
    private Boolean codEnabled;

    /** Upfront online advance required for COD orders on this product (e.g. transport charge). Zero/null = no advance. */
    @DecimalMin(value = "0.0", inclusive = true, message = "COD advance amount must be non-negative")
    private BigDecimal codAdvanceAmount;

    private List<String> imageUrls;

    @Valid
    private List<ProductVariantRequest> variants;

    public ProductRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Boolean getTrending() {
        return trending;
    }

    public void setTrending(Boolean trending) {
        this.trending = trending;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getCodEnabled() {
        return codEnabled;
    }

    public void setCodEnabled(Boolean codEnabled) {
        this.codEnabled = codEnabled;
    }

    public BigDecimal getCodAdvanceAmount() {
        return codAdvanceAmount;
    }

    public void setCodAdvanceAmount(BigDecimal codAdvanceAmount) {
        this.codAdvanceAmount = codAdvanceAmount;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<ProductVariantRequest> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariantRequest> variants) {
        this.variants = variants;
    }
}
