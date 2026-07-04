package com.dreamnest.dto.response;

import java.math.BigDecimal;

/**
 * Response payload summarizing key statistics for a business admin's dashboard.
 */
public class BusinessDashboardStatsResponse {

    private long totalProducts;
    private long totalOrders;
    private long pendingOrders;
    private BigDecimal totalRevenue;
    private long totalCategoriesUsed;
    private long lowStockProducts;

    public BusinessDashboardStatsResponse() {
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalCategoriesUsed() {
        return totalCategoriesUsed;
    }

    public void setTotalCategoriesUsed(long totalCategoriesUsed) {
        this.totalCategoriesUsed = totalCategoriesUsed;
    }

    public long getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(long lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }
}
