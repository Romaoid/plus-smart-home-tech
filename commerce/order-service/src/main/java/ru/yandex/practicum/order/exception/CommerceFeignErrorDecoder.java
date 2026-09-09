package ru.yandex.practicum.order.exception;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CommerceFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new OrderProcessingException("Товар id:%d не найден");
        }

        if (response.status() == 409) {
            return new OrderProcessingException("Товара id:%d в наличии недостаточно");
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
