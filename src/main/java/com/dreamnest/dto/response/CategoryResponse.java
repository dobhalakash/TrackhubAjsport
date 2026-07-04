package com.dreamnest.dto.response;

import com.dreamnest.enums.SportsCategory;

/**
 * Response payload representing a product category.
 */
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private String sizeGuide;
    private SportsCategory categoryType;
    private boolean active;

    public CategoryResponse() {
    }

    public CategoryResponse(Long id, String name, String slug, String description,
                             String imageUrl, SportsCategory categoryType, boolean active) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.imageUrl = imageUrl;
        this.categoryType = categoryType;
        this.active = active;
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSizeGuide() {
        return sizeGuide;
    }

    public void setSizeGuide(String sizeGuide) {
        this.sizeGuide = sizeGuide;
    }

    public SportsCategory getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(SportsCategory categoryType) {
        this.categoryType = categoryType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
