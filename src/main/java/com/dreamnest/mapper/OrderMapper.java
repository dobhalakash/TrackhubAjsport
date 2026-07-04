package com.dreamnest.mapper;

import com.dreamnest.dto.response.OrderItemResponse;
import com.dreamnest.dto.response.OrderResponse;
import com.dreamnest.entity.Order;
import com.dreamnest.entity.OrderItem;
import com.dreamnest.entity.Payment;

import java.util.stream.Collectors;

/**
 * Maps {@link Order} entities to response DTOs.
 */
public class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        return toResponse(order, null);
    }

    public static OrderResponse toResponse(Order order, Payment payment) {
        if (order == null) {
            return null;
        }
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setAddress(AddressMapper.toResponse(order.getAddress()));
        response.setSubtotal(order.getSubtotal());
        response.setTaxAmount(order.getTaxAmount());
        response.setShippingAmount(order.getShippingAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setGrandTotal(order.getGrandTotal());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getItems() != null) {
            response.setItems(order.getItems().stream()
                    .map(OrderMapper::toItemResponse)
                    .collect(Collectors.toList()));
        }

        if (payment != null) {
            response.setPaymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
            response.setPaymentStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
            response.setCodDueAmount(payment.getCodDueAmount());
        }
        response.setCancellationReason(order.getCancellationReason());
        response.setReturnReason(order.getReturnReason());
        response.setCustomerCity(order.getCustomerCity());
        response.setCustomerRegion(order.getCustomerRegion());
        response.setCustomerCountry(order.getCustomerCountry());
        return response;
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        response.setProductName(item.getProductName());
        if (item.getProduct() != null && item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            response.setProductImageUrl(item.getProduct().getImages().get(0).getImageUrl());
        }
        response.setSize(item.getSize());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setTotalPrice(item.getTotalPrice());
        return response;
    }
}
