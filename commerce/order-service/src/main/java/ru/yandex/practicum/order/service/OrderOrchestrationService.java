package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.Item;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.RemoteCallResult;
import ru.yandex.practicum.order.logger.Loggable;
import ru.yandex.practicum.order.mapper.ItemMapper;
import ru.yandex.practicum.order.mapper.OrderMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderOrchestrationService {
    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Loggable
    public OrderDto createOrder(CreateOrderRequest request) {
        List<Item> items = getProducts(request.items());

        ReserveResult reserveResult = reserveProducts(items);

        Order newOrder = checkDegradationAndGetOrder(items, reserveResult);

        return saveOrder(
                OrderMapper.mapToOrder(newOrder, request, items),
                reserveResult);
    }

    @Loggable
    private List<Item> getProducts(List<OrderItemRequest> itemsList) {
        Map<Long, Item> itemsMap = new HashMap<>();

        for (OrderItemRequest item : itemsList) {
            Long productId = item.productId();

            if (itemsMap.containsKey(productId)) {
                Item itemFromMap = itemsMap.get(productId);

                itemFromMap.setQuantity(itemFromMap.getQuantity() + item.quantity());

                itemsMap.put(productId, itemFromMap);
            } else {
                RemoteCallResult<ProductDto> result = getProduct(productId);

                switch (result) {
                    case RemoteCallResult.Success<ProductDto> success -> {
                        ProductDto product = success.value();

                        isProductActiveOrThrow(product);

                        itemsMap.put(productId, ItemMapper.mapToItem(item, product));
                    }
                    case RemoteCallResult.BusinessFailure<ProductDto> failure ->
                        throw new OrderProcessingException(failure.message());
                    case RemoteCallResult.TechnicalFailure<ProductDto> failure ->
                        itemsMap.put(productId, ItemMapper.mapToDegradatedItem(item));
                }
            }
        }

        return itemsMap.values().stream().toList();
    }

    private RemoteCallResult<ProductDto> getProduct(Long productId) {
        try {
            return new RemoteCallResult.Success<>(
                    productClient.getProductById(productId)
            );
        } catch (OrderProcessingException exception) {
            throw new OrderProcessingException(
                    exception.getMessage().formatted(productId)
            );
        } catch (ProductServiceUnavailableException exception) {
            return new RemoteCallResult.TechnicalFailure<>(
                    "Каталог временно недоступен"
            );
        }
    }

    private ReserveResult reserveProducts(List<Item> itemsToReserve) {
        List<ReserveRequest> reservedItems = new ArrayList<>();
        List<Long> unReservedItemIds = new ArrayList<>();
        try {
            for (Item item : itemsToReserve) {

                RemoteCallResult<ReserveRequest> callResult = reserveProduct(item);

                switch (callResult) {
                    case RemoteCallResult.Success<ReserveRequest> success -> {
                        ReserveRequest reserveRequest = success.value();

                        reservedItems.add(reserveRequest);
                    }
                    case RemoteCallResult.BusinessFailure<ReserveRequest> failure ->
                        throw new OrderProcessingException(failure.message());
                    case RemoteCallResult.TechnicalFailure<ReserveRequest> failure ->
                            unReservedItemIds.add(item.getProductId());
                }
            }
            return new ReserveResult(reservedItems, unReservedItemIds);
        } catch (OrderProcessingException e) {
            releaseProducts(reservedItems);
            throw new OrderProcessingException(e.getMessage());
        }
    }

    private RemoteCallResult<ReserveRequest> reserveProduct(Item item) {
        try {
            ReserveRequest reserveRequest = new ReserveRequest(
                    item.getProductId(),
                    item.getQuantity()
            );

            inventoryClient.reserveStock(reserveRequest);

            return new RemoteCallResult.Success<>(
                    reserveRequest
            );

        } catch (OrderProcessingException exception) {
            return new RemoteCallResult.BusinessFailure<>(
                    exception.getMessage().formatted(item.getProductId())
            );

        } catch (InventoryServiceUnavailableException exception) {
            return new RemoteCallResult.TechnicalFailure<>(
                    "Сервис склада временно недоступен"
            );
        }
    }

    private void releaseProducts(List<ReserveRequest> itemsToRelease) {
        if (itemsToRelease.isEmpty()) {
            return;
        }

        try {
            for (ReserveRequest releaseRequest : itemsToRelease) {

                inventoryClient.releaseStock(releaseRequest);

            }
        } catch (InventoryServiceUnavailableException exception) {
            throw new OrderProcessingException("Сервис склада временно недоступен. Снять резерв не удалось");
        }
    }

    private OrderDto saveOrder(Order newOrder, ReserveResult reserveResult) {
        try {
            return orderService.addOrder(newOrder);
        } catch (Exception e) {
            releaseProducts(reserveResult.reservedItems);
            throw new OrderProcessingException("Невозможно оформить заказ, попробуйте позднее");
        }
    }

    private void isProductActiveOrThrow(ProductDto product) {
        if (!product.active()) {
            throw new OrderProcessingException(
                    String.format("Товар id:%d снят с продажи", product.id())
            );
        }
    }

    private Order checkDegradationAndGetOrder(List<Item> items, ReserveResult reserveResult) {
        Order newOrder = new Order();

        String details = items.stream()
                .filter(item -> item.getProductName() == null
                        || item.getPrice().equals(BigDecimal.ZERO)
                        || reserveResult.unReservedItemIds.contains(item.getProductId())
                )
                .map(item -> "Товар #<%d> (ожидает проверки)\n".formatted(item.getProductId()))
                .collect(Collectors.joining());

        if (!details.isEmpty()) {
            newOrder.setStatusDetails(details);
            newOrder.setStatus(OrderStatus.PENDING_CONFIRMATION);
        }

        return newOrder;
    }

    private record ReserveResult(
            List<ReserveRequest> reservedItems,
            List<Long> unReservedItemIds
    ) {}
}
