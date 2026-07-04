package com.dreamnest.entity;

import com.dreamnest.enums.ProductSize;
import jakarta.persistence.*;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private ProductSize size;

    @Column(name = "fit_type", length = 50)
    private String fitType;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "sku_suffix", length = 30)
    private String skuSuffix;

    public ProductVariant() {
    }

    public ProductVariant(Product product, ProductSize size, String fitType, Integer stock, String skuSuffix) {
        this.product = product;
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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductVariant)) return false;
        ProductVariant that = (ProductVariant) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ProductVariant{id=" + id + ", size=" + size + ", fitType='" + fitType + "', stock=" + stock + '}';
    }
}
