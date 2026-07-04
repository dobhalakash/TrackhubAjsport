package com.dreamnest.dto.response;

import com.dreamnest.enums.ProductSize;

import java.math.BigDecimal;

/**
 * Response payload representing a single item in the shopping cart.
 */
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private ProductSize size;
    private Integer quantity;
    private BigDecimal priceAtAdd;
    private BigDecimal lineTotal;
    private boolean savedForLater;
    private Integer availableStock;
    private boolean codEnabled;
    private java.math.BigDecimal codAdvanceAmount;

    public CartItemResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public ProductSize getSize() {
        return size;
    }

    public void setSize(ProductSize size) {
        this.size = size;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtAdd() {
        return priceAtAdd;
    }

    public void setPriceAtAdd(BigDecimal priceAtAdd) {
        this.priceAtAdd = priceAtAdd;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public boolean isSavedForLater() {
        return savedForLater;
    }

    public void setSavedForLater(boolean savedForLater) {
        this.savedForLater = savedForLater;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
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
}
