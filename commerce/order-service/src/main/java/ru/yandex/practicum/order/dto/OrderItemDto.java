package ru.yandex.practicum.order.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price
) {
    public OrderItemDto updateQuantity(Integer quantity) {
        return new OrderItemDto(
                this.id,
                this.productId,
                this.productName,
                this.quantity + quantity,
                this.price
        );
    }
}