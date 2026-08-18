package ru.yandex.practicum.order.mapper;

import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.entity.Item;
import ru.yandex.practicum.order.entity.Order;

import java.util.List;

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
                order.getItems().stream().map(ItemMapper::mapToDto).toList()
        );
    }

    public static Order mapToOrder(Order order, CreateOrderRequest request, List<Item> items) {
        order.setCustomerEmail(request.customerEmail());
        order.setCustomerName(request.customerName());

        items.forEach(order::addItem);

        return order;
    }
}
