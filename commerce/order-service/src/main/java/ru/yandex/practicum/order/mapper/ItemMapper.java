package ru.yandex.practicum.order.mapper;

import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.ProductDto;
import ru.yandex.practicum.order.entity.Item;

public class ItemMapper {
    public static Item mapToItem(OrderItemRequest request, ProductDto product) {
        return Item.builder()
                .productId(product.id())
                .productName(product.name())
                .quantity(request.quantity())
                .price(product.price())
                .build();
    }

    public static OrderItemDto mapToDto(Item item){
        return new OrderItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}
