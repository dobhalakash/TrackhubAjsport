package com.dreamnest.dto.response;

/**
 * Response payload representing a product image.
 */
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private boolean primary;
    private Integer displayOrder;

    public ProductImageResponse() {
    }

    public ProductImageResponse(Long id, String imageUrl, boolean primary, Integer displayOrder) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.primary = primary;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
