package ru.yandex.practicum.order.mapper;

import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Item;
import ru.yandex.practicum.order.entity.Order;

public class OrderMapper {
    public static OrderDto mapToDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus().toString(),
                order.getTotalPrice(),
                order.getStatusDetails(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderMapper::mapItemDto).toList()
        );
    }

    public static Order mapToOrder(Order order, CreateOrderRequest request) {
        order.setCustomerEmail(request.customerEmail());
        order.setCustomerName(request.customerName());

        request.items().stream()
                .map(OrderMapper::mapItem)
                .forEach(order::addItem);

        return order;
    }

    private static OrderItemDto mapItemDto(Item item){
        return new OrderItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
        );
    }

    private static Item mapItem(OrderItemRequest request){
        return Item.builder()
                .productId(request.productId())
                .productName(request.productName())
                .quantity(request.quantity())
                .price(request.price())
                .build();
    }
}
