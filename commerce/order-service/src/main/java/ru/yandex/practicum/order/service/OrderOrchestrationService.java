package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.Item;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.logger.Loggable;
import ru.yandex.practicum.order.mapper.ItemMapper;
import ru.yandex.practicum.order.mapper.OrderMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderOrchestrationService {
    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Loggable
    public OrderDto createOrder(CreateOrderRequest request) {

        List<Item> items = getProducts(request.items());

        reserveProducts(items);

        try {
            return orderService.addOrder(
                    OrderMapper.mapToOrder(new Order(), request, items));
        } catch (Exception e) {
            releaseProducts(items.stream()
                    .map(i -> new ReserveRequest(
                            i.getProductId(),
                            i.getQuantity()))
                    .toList()
            );
            throw new OrderProcessingException("Невозможно оформить заказ, попробуйте позднее");
        }
    }

    @Loggable
    private List<Item> getProducts(List<OrderItemRequest> itemsList) {
        Map<Long, Item> itemsMap = new HashMap<>();
        Long productId = 0L;
        try {
            for (OrderItemRequest item : itemsList) {
                productId = item.productId();

                if (itemsMap.containsKey(productId)) {
                    Item itemFromMap = itemsMap.get(productId);

                    itemFromMap.setQuantity(itemFromMap.getQuantity() + item.quantity());

                    itemsMap.put(productId,itemFromMap);
                } else {
                    ProductDto product = productClient.getProductById(productId);

                    if (!product.active()) {
                        throw new OrderProcessingException("Товар id:%d снят с продажи");
                    }

                    Item newItem = ItemMapper.mapToItem(item, product);
                    itemsMap.put(productId, newItem);
                }
            }

            return itemsMap.values().stream().toList();

        } catch (FeignException.NotFound exception) {
            throw new OrderProcessingException("Товар id:%d не найден".formatted(productId));
        }
    }

    private void reserveProducts(List<Item> itemsToReserve) {
        Long productId = 0L;
        List<ReserveRequest> reservedItems = new ArrayList<>();

        try {
            for (Item item : itemsToReserve) {
                ReserveRequest reserveRequest = new ReserveRequest(
                        item.getProductId(),
                        item.getQuantity()
                );

                productId = item.getProductId();
                inventoryClient.reserveStock(reserveRequest);

                reservedItems.add(reserveRequest);
            }
        } catch (FeignException.Conflict exception) {
            if (!reservedItems.isEmpty()) {
                releaseProducts(reservedItems);
            }

            throw new OrderProcessingException("Товара id %d в наличии недостаточно".formatted(productId));
        } catch (FeignException.NotFound exception) {
            throw new OrderProcessingException("Товар id:%d нет в наличии".formatted(productId));
        } catch (FeignException.ServiceUnavailable exception) {
            throw new OrderProcessingException("Сервис склада временно недоступен");
        }
    }

    private void releaseProducts(List<ReserveRequest> itemsToRelease) {
        try {
            for (ReserveRequest releaseRequest : itemsToRelease) {

                inventoryClient.releaseStock(releaseRequest);

            }
        } catch (FeignException.ServiceUnavailable exception) {
            throw new OrderProcessingException("Сервис склада временно недоступен");
        }
    }
}
