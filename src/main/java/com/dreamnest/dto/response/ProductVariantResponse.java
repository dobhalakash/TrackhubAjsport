package com.dreamnest.dto.response;

import com.dreamnest.enums.ProductSize;

/**
 * Response payload representing a product variant.
 */
public class ProductVariantResponse {

    private Long id;
    private ProductSize size;
    private String fitType;
    private Integer stock;
    private String skuSuffix;

    public ProductVariantResponse() {
    }

    public ProductVariantResponse(Long id, ProductSize size, String fitType, Integer stock, String skuSuffix) {
        this.id = id;
        this.size = size;
        this.fitType = fitType;
        this.stock = stock;
        this.skuSuffix = skuSuffix;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
