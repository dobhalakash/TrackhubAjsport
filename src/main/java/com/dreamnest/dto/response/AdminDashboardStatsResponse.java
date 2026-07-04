package com.dreamnest.dto.response;

import java.math.BigDecimal;

/**
 * Response payload summarizing platform-wide statistics for the super admin dashboard.
 */
public class AdminDashboardStatsResponse {

    private long totalUsers;
    private long totalCustomers;
    private long totalBusinessAccounts;
    private long pendingBusinessApprovals;
    private long totalOrders;
    private long totalProducts;
    private long totalCategories;
    private BigDecimal totalRevenue;

    public AdminDashboardStatsResponse() {
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalBusinessAccounts() {
        return totalBusinessAccounts;
    }

    public void setTotalBusinessAccounts(long totalBusinessAccounts) {
        this.totalBusinessAccounts = totalBusinessAccounts;
    }

    public long getPendingBusinessApprovals() {
        return pendingBusinessApprovals;
    }

    public void setPendingBusinessApprovals(long pendingBusinessApprovals) {
        this.pendingBusinessApprovals = pendingBusinessApprovals;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(long totalCategories) {
        this.totalCategories = totalCategories;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
