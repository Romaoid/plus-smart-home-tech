package ru.yandex.practicum.order.fallback.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.ProductClient;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final Logger log =
            LoggerFactory.getLogger(ProductClientFallbackFactory.class);

    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            if (cause instanceof OrderProcessingException) {
                throw (OrderProcessingException) cause;
            }

            log.warn(
                    "product-service недоступен при запросе товара id={}",
                    productId,
                    cause
            );

            throw new ProductServiceUnavailableException(productId, cause);
        };
    }
}
