package ru.yandex.practicum.order.exception;

import lombok.Getter;

@Getter
public class InventoryServiceUnavailableException extends RuntimeException {
    private final Long productId;

    public InventoryServiceUnavailableException(Long id, Throwable message) {
        super(String.format("Inventory service unavailable for product id: %d", id), message);
        productId = id;
    }
}
