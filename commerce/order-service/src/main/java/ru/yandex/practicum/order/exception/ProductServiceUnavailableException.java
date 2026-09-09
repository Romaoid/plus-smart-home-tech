package ru.yandex.practicum.order.exception;

import lombok.Getter;

@Getter
public class ProductServiceUnavailableException extends RuntimeException {
    private final Long productId;

    public ProductServiceUnavailableException(Long id, Throwable message) {
        super(String.format("Product service unavailable for product id: %d", id), message);
        productId = id;
    }

}
