package com.dreamnest.dto.request;

import com.dreamnest.enums.ProductSize;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload representing a single product variant (size/color/stock).
 */
public class ProductVariantRequest {

    @NotNull(message = "Size is required")
    private ProductSize size;

    private String fitType;

    @NotNull(message = "Stock is required")
    private Integer stock;

    private String skuSuffix;

    public ProductVariantRequest() {
    }

    public ProductSize getSize() {
        return size;
    }

    public void setSize(ProductSize size) {
        this.size = size;
    }

    public String getFitType() {
        return fitType;
    }

    public void setFitType(String fitType) {
        this.fitType = fitType;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getSkuSuffix() {
        return skuSuffix;
    }

    public void setSkuSuffix(String skuSuffix) {
        this.skuSuffix = skuSuffix;
    }
}
