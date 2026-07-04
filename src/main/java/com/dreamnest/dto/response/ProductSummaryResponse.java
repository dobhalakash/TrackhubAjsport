package com.dreamnest.dto.response;

import java.math.BigDecimal;

/**
 * Lightweight response payload for product listings (search/grid views).
 */
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal discountPercentage;
    private BigDecimal finalPrice;
    private String brand;
    private boolean trending;
    private boolean codEnabled;
    private java.math.BigDecimal codAdvanceAmount;
    private Integer stock;
    private BigDecimal averageRating;
    private String primaryImageUrl;
    private String categoryName;

    public ProductSummaryResponse() {
    }

    public ProductSummaryResponse(Long id, String name, BigDecimal price, BigDecimal discountPercentage,
                                   BigDecimal finalPrice, String brand, boolean trending, Integer stock,
                                   BigDecimal averageRating, String primaryImageUrl, String categoryName) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.discountPercentage = discountPercentage;
        this.finalPrice = finalPrice;
        this.brand = brand;
        this.trending = trending;
        this.stock = stock;
        this.averageRating = averageRating;
        this.primaryImageUrl = primaryImageUrl;
        this.categoryName = categoryName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public boolean isTrending() {
        return trending;
    }

    public void setTrending(boolean trending) {
        this.trending = trending;
    }

    public boolean isCodEnabled() {
        return codEnabled;
    }

    public void setCodEnabled(boolean codEnabled) {
        this.codEnabled = codEnabled;
    }

    public java.math.BigDecimal getCodAdvanceAmount() {
        return codAdvanceAmount;
    }

    public void setCodAdvanceAmount(java.math.BigDecimal codAdvanceAmount) {
        this.codAdvanceAmount = codAdvanceAmount;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public String getPrimaryImageUrl() {
        return primaryImageUrl;
    }

    public void setPrimaryImageUrl(String primaryImageUrl) {
        this.primaryImageUrl = primaryImageUrl;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
